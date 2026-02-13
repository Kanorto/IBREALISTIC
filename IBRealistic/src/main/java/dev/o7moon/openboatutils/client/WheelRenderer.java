package dev.o7moon.openboatutils.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders four wheels on the boat when realistic physics is active.
 * Each wheel is composed of four overlapping cuboids rotated 45° apart (0°, 45°, 90°, 135°)
 * to approximate a rounder cross-section (tire), plus an inner hub disc.
 * Front wheels rotate based on steering angle and all wheels spin with velocity.
 *
 * Coordinate system (after scale(-1,-1,1) and rotateY(90) in vanilla renderer):
 * - X: vehicle longitudinal axis (positive = forward)
 * - Y: vehicle vertical axis (positive = down, due to scale -1)
 * - Z: vehicle lateral axis (positive = right)
 */
public class WheelRenderer {

    // ─── WHEEL DIMENSIONS ───
    private static final float WHEEL_RADIUS = 3.0f;        // visual radius in blocks (larger for visibility)
    private static final float WHEEL_Y_OFFSET = 0.20f;     // vertical position below boat center (closer to hull)
    private static final float FRONT_X_OFFSET = 0.6f;      // front axle forward from center
    private static final float REAR_X_OFFSET = -0.6f;      // rear axle behind center
    private static final float LATERAL_OFFSET = 0.7f;      // half track width (tighter to boat body)

    // ─── TIRE MODEL UNITS ───
    // Tire cuboid: width(X) × height(Y) × depth(Z) in model units
    // Two overlapping cuboids rotated 45° create an octagonal profile
    // Block size = model_units * scale, where scale = WHEEL_RADIUS / TIRE_HALF_SIZE
    private static final float TIRE_HALF_WIDTH = 1.5f;     // half thickness along axle (full width ~0.24 blocks)
    private static final float TIRE_HALF_SIZE = 5f;        // half height/depth of tire face (= radius)
    // Hub disc: smaller, slightly wider cuboid at the center
    private static final float HUB_HALF_WIDTH = 1.8f;      // slightly wider than tire to be visible
    private static final float HUB_HALF_SIZE = 2.8f;       // ~56% of tire face for rim look

    // ─── WHEEL SPIN ───
    /** Accumulated spin angle from completed ticks (degrees) */
    private static volatile float wheelSpinAngleTick = 0f;
    /** Forward speed snapshot from the last tick for interpolation */
    private static volatile float lastTickSpeed = 0f;
    private static final float SPIN_SPEED_FACTOR = 200.0f; // degrees per (m/s) per tick
    private static final float TICK_TIME = 0.05f;          // seconds per game tick (1/20)

    // ─── HANDBRAKE LOCK ───
    /** Captured rear wheel spin angle when handbrake was engaged (degrees) */
    private static volatile float lockedRearSpinAngle = 0f;
    /** Whether the rear wheels were locked on the previous frame */
    private static volatile boolean wasHandbrakeActive = false;

    // ─── CACHED MODEL PARTS ───
    private static ModelPart wheelModel = null;

    // ─── TEXTURES ───
    private static final Identifier TIRE_TEXTURE =
            Identifier.of("minecraft", "textures/block/black_concrete.png");
    private static final Identifier HUB_TEXTURE =
            Identifier.of("minecraft", "textures/block/gray_concrete.png");

    /**
     * Creates a compound wheel model with four overlapping tire cuboids (0°, 45°, 90°, 135°)
     * and a central hub disc for a more realistic round appearance.
     */
    private static ModelPart getOrCreateWheelModel() {
        if (wheelModel == null) {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();

            // Primary tire cuboid (0° orientation)
            root.addChild("tire_0",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-TIRE_HALF_WIDTH, -TIRE_HALF_SIZE, -TIRE_HALF_SIZE,
                                    TIRE_HALF_WIDTH * 2f, TIRE_HALF_SIZE * 2f, TIRE_HALF_SIZE * 2f),
                    ModelTransform.NONE);

            // Second tire cuboid rotated 45° around X axis
            root.addChild("tire_45",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-TIRE_HALF_WIDTH, -TIRE_HALF_SIZE, -TIRE_HALF_SIZE,
                                    TIRE_HALF_WIDTH * 2f, TIRE_HALF_SIZE * 2f, TIRE_HALF_SIZE * 2f),
                    ModelTransform.rotation((float) Math.toRadians(45), 0f, 0f));

            // Third tire cuboid rotated 90° around X axis
            root.addChild("tire_90",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-TIRE_HALF_WIDTH, -TIRE_HALF_SIZE, -TIRE_HALF_SIZE,
                                    TIRE_HALF_WIDTH * 2f, TIRE_HALF_SIZE * 2f, TIRE_HALF_SIZE * 2f),
                    ModelTransform.rotation((float) Math.toRadians(90), 0f, 0f));

            // Fourth tire cuboid rotated 135° around X axis (completes ~16-gon profile)
            root.addChild("tire_135",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-TIRE_HALF_WIDTH, -TIRE_HALF_SIZE, -TIRE_HALF_SIZE,
                                    TIRE_HALF_WIDTH * 2f, TIRE_HALF_SIZE * 2f, TIRE_HALF_SIZE * 2f),
                    ModelTransform.rotation((float) Math.toRadians(135), 0f, 0f));

            // Central hub/rim disc (smaller, slightly wider)
            root.addChild("hub",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-HUB_HALF_WIDTH, -HUB_HALF_SIZE, -HUB_HALF_SIZE,
                                    HUB_HALF_WIDTH * 2f, HUB_HALF_SIZE * 2f, HUB_HALF_SIZE * 2f),
                    ModelTransform.NONE);

            wheelModel = TexturedModelData.of(modelData, 32, 32).createModel();
        }
        return wheelModel;
    }

    /**
     * Updates wheel spin angle. Must be called once per game tick (not per frame).
     *
     * @param forwardSpeed current forward velocity in m/s
     */
    public static void tickWheelSpin(float forwardSpeed) {
        lastTickSpeed = forwardSpeed;
        wheelSpinAngleTick += forwardSpeed * SPIN_SPEED_FACTOR * TICK_TIME;
        wheelSpinAngleTick = ((wheelSpinAngleTick % 360f) + 360f) % 360f;
    }

    /**
     * Renders four wheels on the boat.
     * Must be called within the boat's render context (after scale and rotateY(90)).
     *
     * @param matrices      the matrix stack in the boat's local coordinate space
     * @param vertexConsumers vertex consumer provider
     * @param light         packed light value
     * @param steeringAngle current steering angle in radians
     * @param forwardSpeed  forward velocity in m/s for wheel spin
     * @param tickDelta     partial tick for smooth interpolation (0.0 to 1.0)
     * @param handbrake     whether the handbrake is engaged (locks rear wheels)
     */
    public static void renderWheels(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, float steeringAngle, float forwardSpeed, float tickDelta,
                                     boolean handbrake) {
        // Interpolate spin angle: base tick angle + fractional tick spin
        float interpolatedSpin = wheelSpinAngleTick + forwardSpeed * SPIN_SPEED_FACTOR * TICK_TIME * tickDelta;

        ModelPart wheel = getOrCreateWheelModel();

        // Two render layers: dark tire and lighter hub
        VertexConsumer tireConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(TIRE_TEXTURE));
        VertexConsumer hubConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(HUB_TEXTURE));

        float steeringDegrees = (float) Math.toDegrees(steeringAngle);

        // Handbrake locks rear wheels: freeze at current angle when first engaged
        float rearSpin;
        if (handbrake) {
            if (!wasHandbrakeActive) {
                lockedRearSpinAngle = interpolatedSpin;
                wasHandbrakeActive = true;
            }
            rearSpin = lockedRearSpinAngle;
        } else {
            wasHandbrakeActive = false;
            rearSpin = interpolatedSpin;
        }

        // Negate steering and spin to compensate for scale(-1,-1,1) inversion
        float visualSteeringDeg = -steeringDegrees;
        float visualFrontSpin = -interpolatedSpin;
        float visualRearSpin = -rearSpin;

        // Render each wheel (X = longitudinal, Z = lateral after vanilla transforms)
        // Front-Left
        renderSingleWheel(matrices, wheel, tireConsumer, hubConsumer, light,
                FRONT_X_OFFSET, WHEEL_Y_OFFSET, -LATERAL_OFFSET,
                visualSteeringDeg, visualFrontSpin);

        // Front-Right
        renderSingleWheel(matrices, wheel, tireConsumer, hubConsumer, light,
                FRONT_X_OFFSET, WHEEL_Y_OFFSET, LATERAL_OFFSET,
                visualSteeringDeg, visualFrontSpin);

        // Rear-Left
        renderSingleWheel(matrices, wheel, tireConsumer, hubConsumer, light,
                REAR_X_OFFSET, WHEEL_Y_OFFSET, -LATERAL_OFFSET,
                0f, visualRearSpin);

        // Rear-Right
        renderSingleWheel(matrices, wheel, tireConsumer, hubConsumer, light,
                REAR_X_OFFSET, WHEEL_Y_OFFSET, LATERAL_OFFSET,
                0f, visualRearSpin);
    }

    /**
     * Renders a single wheel at the specified position with steering and spin rotation.
     * Draws tire cuboids in dark color and hub disc in lighter color.
     * A base 90° Y rotation orients the tire face along the boat sides.
     */
    private static void renderSingleWheel(MatrixStack matrices, ModelPart wheel,
                                           VertexConsumer tireConsumer, VertexConsumer hubConsumer,
                                           int light,
                                           float x, float y, float z,
                                           float steeringDeg, float spinDeg) {
        matrices.push();

        // Position the wheel
        matrices.translate(x, y, z);

        // Scale from model units to block units
        // TIRE_HALF_SIZE = half the cuboid face size, so scale = radius / halfSize
        float scale = WHEEL_RADIUS / TIRE_HALF_SIZE;
        matrices.scale(scale, scale, scale);

        // Base 90° Y rotation to orient tire face along the boat sides
        // (compensates for vanilla renderer's rotateY(90) transform)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));

        // Apply steering rotation (Y axis for turning left/right)
        if (Math.abs(steeringDeg) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(steeringDeg));
        }

        // Apply spin rotation (X axis for rolling forward/backward)
        if (Math.abs(spinDeg) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spinDeg));
        }

        // Render tire cuboids (dark rubber) — 4 overlapping cuboids for round profile
        //? <=1.20.4 {
        wheel.getChild("tire_0").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0.12f, 0.12f, 0.12f, 1.0f);
        wheel.getChild("tire_45").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0.12f, 0.12f, 0.12f, 1.0f);
        wheel.getChild("tire_90").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0.12f, 0.12f, 0.12f, 1.0f);
        wheel.getChild("tire_135").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0.12f, 0.12f, 0.12f, 1.0f);
        // Render hub disc (lighter rim)
        wheel.getChild("hub").render(matrices, hubConsumer, light, OverlayTexture.DEFAULT_UV,
                0.35f, 0.35f, 0.35f, 1.0f);
        //?}
        //? >=1.21 {
        /*wheel.getChild("tire_0").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF1F1F1F);
        wheel.getChild("tire_45").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF1F1F1F);
        wheel.getChild("tire_90").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF1F1F1F);
        wheel.getChild("tire_135").render(matrices, tireConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF1F1F1F);
        // Render hub disc (lighter rim)
        wheel.getChild("hub").render(matrices, hubConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF595959);
        *///?}

        matrices.pop();
    }
}
