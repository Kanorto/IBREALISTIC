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
 * Wheels are small cubes positioned at the four corners of the vehicle,
 * with front wheels rotating based on steering angle and all wheels
 * spinning based on forward velocity.
 *
 * Coordinate system (after scale(-1,-1,1) and rotateY(90) in vanilla renderer):
 * - X: vehicle lateral axis (positive = left)
 * - Y: vehicle vertical axis (positive = down, due to scale -1)
 * - Z: vehicle longitudinal axis (positive = forward)
 */
public class WheelRenderer {

    // ─── WHEEL DIMENSIONS ───
    private static final float WHEEL_RADIUS = 0.15f;     // visual radius in blocks
    private static final float WHEEL_WIDTH = 0.1f;       // wheel thickness
    private static final float WHEEL_Y_OFFSET = 0.3f;    // vertical position below boat center
    private static final float FRONT_Z_OFFSET = 0.55f;   // front axle forward from center
    private static final float REAR_Z_OFFSET = -0.55f;   // rear axle behind center
    private static final float LATERAL_OFFSET = 0.5f;    // half track width

    // ─── WHEEL SPIN ───
    /** Accumulated spin angle from completed ticks (degrees) */
    private static volatile float wheelSpinAngleTick = 0f;
    /** Forward speed snapshot from the last tick for interpolation */
    private static volatile float lastTickSpeed = 0f;
    private static final float SPIN_SPEED_FACTOR = 200.0f; // degrees per (m/s) per tick
    private static final float TICK_TIME = 0.05f; // seconds per game tick (1/20)

    // ─── CACHED MODEL PARTS ───
    private static ModelPart wheelModel = null;

    /**
     * Creates a simple wheel model part (a flat cuboid).
     */
    private static ModelPart getOrCreateWheelModel() {
        if (wheelModel == null) {
            // Create a simple cuboid model part for a wheel
            // Wheel is a disc-like shape: thin in width, roughly square in height/depth
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();

            // Create a small cuboid representing a wheel
            // Size: width x height x depth (in 1/16 block units for model, but we scale it)
            root.addChild("wheel",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-2f, -3f, -3f, 4f, 6f, 6f),
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
     * @param matrices     the matrix stack in the boat's local coordinate space
     * @param vertexConsumers vertex consumer provider
     * @param light        packed light value
     * @param steeringAngle current steering angle in radians
     * @param forwardSpeed  forward velocity in m/s for wheel spin
     * @param tickDelta     partial tick for smooth interpolation (0.0 to 1.0)
     */
    public static void renderWheels(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, float steeringAngle, float forwardSpeed, float tickDelta) {
        // Interpolate spin angle: base tick angle + fractional tick spin
        float interpolatedSpin = wheelSpinAngleTick + forwardSpeed * SPIN_SPEED_FACTOR * TICK_TIME * tickDelta;

        ModelPart wheel = getOrCreateWheelModel();
        // Use entity_solid render layer with white texture
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(Identifier.of("minecraft", "textures/block/black_concrete.png")));

        float steeringDegrees = (float) Math.toDegrees(steeringAngle);

        // Render each wheel
        // Front-Left
        renderSingleWheel(matrices, wheel, vertexConsumer, light,
                -LATERAL_OFFSET, WHEEL_Y_OFFSET, FRONT_Z_OFFSET,
                steeringDegrees, interpolatedSpin);

        // Front-Right
        renderSingleWheel(matrices, wheel, vertexConsumer, light,
                LATERAL_OFFSET, WHEEL_Y_OFFSET, FRONT_Z_OFFSET,
                steeringDegrees, interpolatedSpin);

        // Rear-Left
        renderSingleWheel(matrices, wheel, vertexConsumer, light,
                -LATERAL_OFFSET, WHEEL_Y_OFFSET, REAR_Z_OFFSET,
                0f, interpolatedSpin);

        // Rear-Right
        renderSingleWheel(matrices, wheel, vertexConsumer, light,
                LATERAL_OFFSET, WHEEL_Y_OFFSET, REAR_Z_OFFSET,
                0f, interpolatedSpin);
    }

    /**
     * Renders a single wheel at the specified position with steering and spin rotation.
     */
    private static void renderSingleWheel(MatrixStack matrices, ModelPart wheel,
                                           VertexConsumer vertexConsumer, int light,
                                           float x, float y, float z,
                                           float steeringDeg, float spinDeg) {
        matrices.push();

        // Position the wheel
        matrices.translate(x, y, z);

        // Scale down from model units to block units
        float scale = WHEEL_RADIUS / 3.0f; // 3.0 = half the cuboid height (6/2)
        matrices.scale(scale, scale, scale);

        // Apply steering rotation (Y axis for turning left/right)
        if (Math.abs(steeringDeg) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(steeringDeg));
        }

        // Apply spin rotation (X axis for rolling forward/backward)
        if (Math.abs(spinDeg) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spinDeg));
        }

        // Render the wheel model part
        //? <=1.20.4 {
        wheel.getChild("wheel").render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV,
                0.15f, 0.15f, 0.15f, 1.0f);
        //?}
        //? >=1.21 {
        /*wheel.getChild("wheel").render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF262626);
        *///?}

        matrices.pop();
    }
}
