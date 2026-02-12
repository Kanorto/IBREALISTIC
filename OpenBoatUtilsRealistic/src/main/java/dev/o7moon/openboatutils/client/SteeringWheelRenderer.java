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
 * Renders a steering wheel on the boat when realistic physics is active.
 * The wheel is tilted at 45° (steering column angle) and rotates to reflect
 * the actual physics steering angle including oversteering and limits.
 *
 * Coordinate system (after scale(-1,-1,1) and rotateY(90) in vanilla renderer):
 * - X: vehicle longitudinal axis (positive = forward)
 * - Y: vehicle vertical axis (positive = down, due to scale -1)
 * - Z: vehicle lateral axis (positive = right)
 */
public class SteeringWheelRenderer {

    // ─── STEERING WHEEL POSITION ───
    private static final float WHEEL_X_OFFSET = 0.45f;     // forward on boat (closer to center)
    private static final float WHEEL_Y_OFFSET = -0.35f;    // raised above boat center (negative = up in inverted Y)
    private static final float WHEEL_Z_OFFSET = 0.0f;      // centered laterally

    // ─── STEERING WHEEL DIMENSIONS ───
    private static final float WHEEL_VISUAL_RADIUS = 2.5f;  // visual radius in blocks
    private static final float COLUMN_TILT_DEGREES = 45f;   // steering column angle toward player

    // ─── MODEL UNITS ───
    private static final float RIM_HALF_SIZE = 5f;          // half size of the rim ring
    private static final float RIM_THICKNESS = 0.6f;        // thickness of the rim ring
    private static final float SPOKE_HALF_WIDTH = 0.5f;     // half width of each spoke
    private static final float SPOKE_HALF_DEPTH = 0.4f;     // half depth of each spoke
    private static final float HUB_SIZE = 1.5f;             // hub center size

    // ─── STEERING RATIO ───
    /** Visual rotation multiplier: real car steering wheels turn ~1.5 full turns lock-to-lock */
    private static final float STEERING_VISUAL_RATIO = 2.5f;

    // ─── CACHED MODEL PARTS ───
    private static ModelPart steeringWheelModel = null;

    // ─── TEXTURES ───
    private static final Identifier RIM_TEXTURE =
            Identifier.of("minecraft", "textures/block/dark_oak_planks.png");
    private static final Identifier HUB_TEXTURE =
            Identifier.of("minecraft", "textures/block/gray_concrete.png");
    /** Top spoke uses a different color to indicate neutral/center position */
    private static final Identifier MARKER_SPOKE_TEXTURE =
            Identifier.of("minecraft", "textures/block/red_concrete.png");

    /**
     * Creates a steering wheel model with a circular rim (approximated by 8 segments),
     * three spokes, and a central hub for a recognizable shape.
     * The wheel face lies in the XZ plane with Y as the column (rotation) axis.
     */
    private static ModelPart getOrCreateModel() {
        if (steeringWheelModel == null) {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();

            // ── RIM SEGMENTS (8 cuboids arranged in a circle in XZ plane) ──
            int segments = 8;
            float segLength = RIM_HALF_SIZE * (float) Math.sin(Math.PI / segments);
            for (int i = 0; i < segments; i++) {
                float angle = (float) (i * 2.0 * Math.PI / segments);
                float cx = (float) Math.cos(angle) * (RIM_HALF_SIZE - segLength * 0.5f);
                float cz = (float) Math.sin(angle) * (RIM_HALF_SIZE - segLength * 0.5f);
                root.addChild("rim_" + i,
                        ModelPartBuilder.create()
                                .uv(0, 0)
                                .cuboid(-segLength, -RIM_THICKNESS, -RIM_THICKNESS,
                                        segLength * 2f, RIM_THICKNESS * 2f, RIM_THICKNESS * 2f),
                        ModelTransform.of(cx, 0f, cz,
                                0f, angle + (float) (Math.PI / 2.0), 0f));
            }

            // ── THREE SPOKES (at 0°, 120°, 240° in XZ plane) ──
            // Each spoke connects the hub (radius ~1.5) to the rim (radius ~4.0)
            float spokeLength = RIM_HALF_SIZE - HUB_SIZE - 0.5f;   // ~3.0 model units
            float spokeCenter = HUB_SIZE + spokeLength * 0.5f;      // center at ~3.0 from origin
            for (int i = 0; i < 3; i++) {
                float angle = (float) (i * 2.0 * Math.PI / 3.0);
                float cx = (float) Math.cos(angle) * spokeCenter;
                float cz = (float) Math.sin(angle) * spokeCenter;
                root.addChild("spoke_" + i,
                        ModelPartBuilder.create()
                                .uv(0, 0)
                                .cuboid(-spokeLength * 0.5f, -SPOKE_HALF_WIDTH, -SPOKE_HALF_DEPTH,
                                        spokeLength, SPOKE_HALF_WIDTH * 2f, SPOKE_HALF_DEPTH * 2f),
                        ModelTransform.of(cx, 0f, cz,
                                0f, angle, 0f));
            }

            // ── CENTRAL HUB ──
            root.addChild("hub",
                    ModelPartBuilder.create()
                            .uv(0, 0)
                            .cuboid(-HUB_SIZE, -HUB_SIZE * 0.5f, -HUB_SIZE,
                                    HUB_SIZE * 2f, HUB_SIZE, HUB_SIZE * 2f),
                    ModelTransform.NONE);

            steeringWheelModel = TexturedModelData.of(modelData, 16, 16).createModel();
        }
        return steeringWheelModel;
    }

    /**
     * Renders the steering wheel on the boat.
     * Must be called within the boat's render context (after scale and rotateY(90)).
     *
     * @param matrices        the matrix stack in the boat's local coordinate space
     * @param vertexConsumers vertex consumer provider
     * @param light           packed light value
     * @param steeringAngle   current physics steering angle in radians
     */
    public static void renderSteeringWheel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                            int light, float steeringAngle) {
        ModelPart wheel = getOrCreateModel();

        VertexConsumer rimConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(RIM_TEXTURE));
        VertexConsumer hubConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(HUB_TEXTURE));
        VertexConsumer markerConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(MARKER_SPOKE_TEXTURE));

        matrices.push();

        // Position the steering wheel on the boat
        matrices.translate(WHEEL_X_OFFSET, WHEEL_Y_OFFSET, WHEEL_Z_OFFSET);

        // Scale from model units to block units
        float scale = WHEEL_VISUAL_RADIUS / RIM_HALF_SIZE;
        matrices.scale(scale, scale, scale);

        // Tilt steering column 45° toward the player (rotate around Z axis)
        // In inverted Y space, negative Z rotation tilts the column backward toward the player
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-COLUMN_TILT_DEGREES));

        // Apply steering rotation around the column axis (Y in model space)
        // Negate to compensate for scale(-1,-1,1) inversion in vanilla renderer
        float visualRotation = -steeringAngle * STEERING_VISUAL_RATIO;
        float visualRotationDeg = (float) Math.toDegrees(visualRotation);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(visualRotationDeg));

        // Render all parts
        for (int i = 0; i < 8; i++) {
            //? <=1.20.4 {
            wheel.getChild("rim_" + i).render(matrices, rimConsumer, light, OverlayTexture.DEFAULT_UV,
                    0.25f, 0.15f, 0.05f, 1.0f);
            //?}
            //? >=1.21 {
            /*wheel.getChild("rim_" + i).render(matrices, rimConsumer, light, OverlayTexture.DEFAULT_UV,
                    0xFF402610);
            *///?}
        }

        // Render spokes: spoke_0 (top/12 o'clock) uses marker color to indicate neutral position
        for (int i = 0; i < 3; i++) {
            VertexConsumer spokeConsumer = (i == 0) ? markerConsumer : rimConsumer;
            //? <=1.20.4 {
            if (i == 0) {
                wheel.getChild("spoke_" + i).render(matrices, spokeConsumer, light, OverlayTexture.DEFAULT_UV,
                        0.8f, 0.15f, 0.1f, 1.0f);
            } else {
                wheel.getChild("spoke_" + i).render(matrices, spokeConsumer, light, OverlayTexture.DEFAULT_UV,
                        0.25f, 0.15f, 0.05f, 1.0f);
            }
            //?}
            //? >=1.21 {
            /*if (i == 0) {
                wheel.getChild("spoke_" + i).render(matrices, spokeConsumer, light, OverlayTexture.DEFAULT_UV,
                        0xFFCC2618);
            } else {
                wheel.getChild("spoke_" + i).render(matrices, spokeConsumer, light, OverlayTexture.DEFAULT_UV,
                        0xFF402610);
            }
            *///?}
        }

        //? <=1.20.4 {
        wheel.getChild("hub").render(matrices, hubConsumer, light, OverlayTexture.DEFAULT_UV,
                0.35f, 0.35f, 0.35f, 1.0f);
        //?}
        //? >=1.21 {
        /*wheel.getChild("hub").render(matrices, hubConsumer, light, OverlayTexture.DEFAULT_UV,
                0xFF595959);
        *///?}

        matrices.pop();
    }
}
