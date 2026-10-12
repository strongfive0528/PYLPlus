package com.connect_screen.mirror.job;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LandscapeAutoScaler {
    public final float[] landscapeMvpMatrix;
    private final float[] identityMvpMatrix;
    private final ExternalTextureRenderer externalTextureRenderer;
    private final int width;
    private final int height;
    private final int fbo;
    private int frameCounter;
    private boolean hasSymmetricBlackBar = false;
    private int topBottomBlackBarSize = 0;
    private int leftRightBlackBarSize = 0;
    // 当前生效的缩放系数，供输入坐标做逆变换使用。渲染线程写入，输入线程读取。
    private volatile float currentScale = 1.0f;
    // 强制缩放模式：只允许建立与保持缩放，不允许回退到未缩放状态。
    private boolean forcedScale = false;
    // 强制缩放模式下已经建立起来的缩放系数。
    private float latchedScale = 1.0f;

    public void setForcedScale(boolean forced) {
        forcedScale = forced;
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public LandscapeAutoScaler(ExternalTextureRenderer externalTextureRenderer, int width, int height, int fbo) {
        this.externalTextureRenderer = externalTextureRenderer;
        this.width = width;
        this.height = height;
        this.fbo = fbo;
        landscapeMvpMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(landscapeMvpMatrix, 0);
        android.opengl.Matrix.scaleM(landscapeMvpMatrix, 0, 1, 1, 1.0f);

        identityMvpMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(identityMvpMatrix, 0);
        android.opengl.Matrix.scaleM(identityMvpMatrix, 0, 1, 1, 1.0f);
    }

    public void onFrame() {
        // 强制缩放模式只做几何计算，不读像素；自动缩放模式维持原有的周期性探测。
        boolean shouldAdjust = forcedScale ? frameCounter % 10 == 0 : frameCounter == 0;
        if (shouldAdjust) {
            adjustLandscapeMvpMatrix();
        }
        frameCounter = (frameCounter + 1) % 300;
    }

    private void adjustLandscapeMvpMatrix() {
        if (forcedScale) {
            // 强制缩放：放大倍数直接由镜像几何算出，与像素探测无关，因此不存在探测失败的情况，
            // 连接后立即生效并恒定保持；输入坐标用同一个系数做逆变换，两者始终一致。
            float forcedValue = SunshineMouse.computeForcedScale();
            if (forcedValue < 1.0f) {
                forcedValue = 1.0f;
            }
            latchedScale = forcedValue;
            currentScale = forcedValue;
            android.opengl.Matrix.setIdentityM(landscapeMvpMatrix, 0);
            android.opengl.Matrix.scaleM(landscapeMvpMatrix, 0, forcedValue, forcedValue, 1.0f);
            return;
        }

        detectBlackBar();
        float scale = 1.0f;
        if (hasSymmetricBlackBar) {
            // 计算缩放比例
            float scaleX = (float)(width) / (width - 2 * leftRightBlackBarSize);
            float scaleY = (float)(height) / (height - 2 * topBottomBlackBarSize);
            scale = Math.min(scaleX, scaleY);

            android.util.Log.d("MirrorActivity", String.format(
                    "应用缩放变换: scaleX=%.2f, scaleY=%.2f, 最终scale=%.2f",
                    scaleX, scaleY, scale
            ));
        }

        if (scale > 1.0f) {
            android.opengl.Matrix.setIdentityM(landscapeMvpMatrix, 0);
            android.opengl.Matrix.scaleM(landscapeMvpMatrix, 0, scale, scale, 1.0f);
        } else {
            for(int i = 0; i < identityMvpMatrix.length; i++) {
                landscapeMvpMatrix[i] = identityMvpMatrix[i];
            }
        }
        currentScale = scale;
    }


    private void detectBlackBar() {
        // 切换到FBO
        if (fbo != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        }

        // 渲染到FBO
        externalTextureRenderer.renderFrame(identityMvpMatrix);

        // 确保渲染完成
        GLES20.glFinish();


        // 检测左右黑边
        ByteBuffer horizontalPixelBuffer = ByteBuffer.allocateDirect(width * 4);
        horizontalPixelBuffer.order(ByteOrder.nativeOrder());

        // 检测上下黑边
        ByteBuffer verticalPixelBuffer = ByteBuffer.allocateDirect(height * 4);
        verticalPixelBuffer.order(ByteOrder.nativeOrder());
        int middleY = height / 2;
        GLES20.glReadPixels(0, middleY, width, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, horizontalPixelBuffer);

        int middleX = width / 2;
        GLES20.glReadPixels(middleX, 0, 1, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, verticalPixelBuffer);

        // 分析水平和垂直像素
        byte[] horizontalPixels = new byte[width * 4];
        byte[] verticalPixels = new byte[height * 4];
        horizontalPixelBuffer.get(horizontalPixels);
        verticalPixelBuffer.get(verticalPixels);

        // 计算左右黑边宽度
        int leftBlackWidth = 0;
        int rightBlackWidth = 0;
        // 计算上下黑边高度
        int topBlackHeight = 0;
        int bottomBlackHeight = 0;

        // 从左向右扫描左黑边
        for (int i = 0; i < width * 4; i += 4) {
            if (isBlackPixel(horizontalPixels[i], horizontalPixels[i+1], horizontalPixels[i+2])) {
                leftBlackWidth++;
            } else {
                break;
            }
        }

        // 从右向左扫描右黑边
        for (int i = width * 4 - 4; i >= 0; i -= 4) {
            if (isBlackPixel(horizontalPixels[i], horizontalPixels[i+1], horizontalPixels[i+2])) {
                rightBlackWidth++;
            } else {
                break;
            }
        }

        // 从下向上扫描底部黑边
        for (int i = 0; i < height * 4; i += 4) {
            if (isBlackPixel(verticalPixels[i], verticalPixels[i+1], verticalPixels[i+2])) {
                bottomBlackHeight++;
            } else {
                break;
            }
        }

        // 从上向下扫描顶部黑边
        for (int i = height * 4 - 4; i >= 0; i -= 4) {
            if (isBlackPixel(verticalPixels[i], verticalPixels[i+1], verticalPixels[i+2])) {
                topBlackHeight++;
            } else {
                break;
            }
        }

        // 判断是否存在对称的黑边
        boolean hasSymmetricHorizontalBars = Math.abs(leftBlackWidth - rightBlackWidth) <= 2
                && leftBlackWidth > 0 && rightBlackWidth > 0;
        boolean hasSymmetricVerticalBars = Math.abs(topBlackHeight - bottomBlackHeight) <= 2
                && topBlackHeight > 0 && bottomBlackHeight > 0;

        android.util.Log.d("MirrorActivity", String.format(
                "左黑边: %d, 右黑边: %d, 上黑边: %d, 下黑边: %d, 水平对称: %b, 垂直对称: %b",
                leftBlackWidth, rightBlackWidth, topBlackHeight, bottomBlackHeight,
                hasSymmetricHorizontalBars, hasSymmetricVerticalBars));

        int horizontalThreshold = (int) (width * 0.3);
        int verticalThreshold = (int) (height * 0.3);
        if (leftBlackWidth < horizontalThreshold && rightBlackWidth < horizontalThreshold && topBlackHeight < verticalThreshold && bottomBlackHeight < verticalThreshold) {
            if (hasSymmetricHorizontalBars && hasSymmetricVerticalBars) {
                hasSymmetricBlackBar = true;
                leftRightBlackBarSize = Math.min(leftBlackWidth, rightBlackWidth);
                topBottomBlackBarSize = Math.min(topBlackHeight, bottomBlackHeight);
            } else {
                if (hasSymmetricHorizontalBars && Math.min(leftBlackWidth, rightBlackWidth) >= leftRightBlackBarSize && Math.min(topBlackHeight, bottomBlackHeight) >= topBottomBlackBarSize) {
                    // keep old value
                } else {
                    hasSymmetricBlackBar = false;
                    leftRightBlackBarSize = 0;
                    topBottomBlackBarSize = 0;
                }
            }
        }
        // 切回默认帧缓冲
        if (fbo != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private boolean isBlackPixel(byte r, byte g, byte b) {
        return (r & 0xFF) == 0 && (g & 0xFF) == 0 && (b & 0xFF) == 0;
    }

    public void exitScale() {
        if (forcedScale) {
            // 强制缩放模式下不允许退出缩放。
            return;
        }
        if (!hasSymmetricBlackBar) {
            return;
        }
        hasSymmetricBlackBar = false;
        currentScale = 1.0f;
        Log.d("LandscapeAutoScaler", "exitScale");
        // 如果没有对称黑边，使用单位矩阵
        for(int i = 0; i < identityMvpMatrix.length; i++) {
            landscapeMvpMatrix[i] = identityMvpMatrix[i];
        }
    }
}
