package com.pkjm.thaw.analyser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.TextureView;

import com.pkjm.thaw.camera2.Camera2BasicFragment;

public class ColorAnalyser {
    private Camera2BasicFragment fragment;
    private PixelDrawer pixelDrawer;

    public ColorAnalyser(Camera2BasicFragment fragment) {
        this.fragment = fragment;
        this.pixelDrawer = new PixelDrawer();
    }

    private final int spanSize = 300;
    private final int stepSize = 60;
    private final int pixelSize = 10;
    private int[] rgb = new int[3];
    private float[] hsv = new float[3];
    private float calS;
    private float calV;

    public void whiteCal(){
        int color = 0;

        if(!fragment.getTextureView().isAvailable()) return;
        try {
            Bitmap bitmap = fragment.getTextureView().getBitmap();
            int cx = bitmap.getWidth() / 2;
            int cy = bitmap.getHeight() / 2;
            double count = 0;
            double sumR = 0, sumG = 0, sumB = 0, adj = 1;

            pixelDrawer.init(fragment.getUpperTextureView());
            for (int i = cx - spanSize; i <= cx + spanSize; i += stepSize) {
                for (int j = cy - spanSize; j <= cy + spanSize; j += stepSize) {
                    color = bitmap.getPixel(i, j);
                    colorToRGB(color, rgb);
                    sumR += rgb[0] * adj;
                    sumG += rgb[1] * adj;
                    sumB += rgb[2] * adj;
                    count += adj;
                    pixelDrawer.draw(i,j,color);
                }
            }
            pixelDrawer.post();

            rgb[0] = (int)(sumR / count);
            rgb[1] = (int)(sumG / count);
            rgb[2] = (int)(sumB / count);
            color = RGBToColor(rgb);

            Color.colorToHSV(color,hsv);
            calS = hsv[1];
            calV = hsv[2];
        }catch (Exception e){

        }
    }

    public int getCalcColor(byte[] udpout){
        int color = 0;

        if(!fragment.getTextureView().isAvailable()) return color;
        try {
            Bitmap bitmap = fragment.getTextureView().getBitmap();
            int cx = bitmap.getWidth() / 2;
            int cy = bitmap.getHeight() / 2;
            double count = 0;
            double sumR=0,sumG=0,sumB=0,adj=1;

            int whitePixelCount = 0;
            int sumWhiteX = 0;
            int sumWhiteY = 0;

            pixelDrawer.init(fragment.getUpperTextureView());
            for (int i = cx-spanSize; i <= cx+spanSize; i += stepSize){
                for (int j = cy-spanSize; j <= cy+spanSize; j += stepSize){
                    color = bitmap.getPixel(i, j);
                    colorToRGB(color, rgb);
                    sumR += rgb[0]*adj;
                    sumG += rgb[1]*adj;
                    sumB += rgb[2]*adj;
                    count += adj;

                    Color.colorToHSV(color,hsv);

                    if (hsv[1] < calS && hsv[2] > calV){
                        sumWhiteX += i - cx;
                        sumWhiteY += j - cy;
                        whitePixelCount++;
                        pixelDrawer.draw(i,j,Color.BLUE);
//                    }
//                    else if (rgb[0] > 140 && rgb[1] > 140 && rgb[2] > 140) {
//                        pixelDrawer.draw(i,j,Color.RED);
                    }else {
                        pixelDrawer.draw(i,j,color);
                    }
                }
            }
            pixelDrawer.post();

//            int state = 0;
//            if (0.75 * count < whitePixelCount) {
//                state = 2;
//            } else if (0.25 * count < whitePixelCount) {
//                state = 1;
//            }

            rgb[0] = (int)(sumR / count);
            rgb[1] = (int)(sumG / count);
            rgb[2] = (int)(sumB / count);

            if (true) { // Determine exact color
                rgb[0] = (rgb[0] > 100) ? 255 : 0;
                rgb[1] = (rgb[1] > 100) ? 255 : 0;
                rgb[2] = (rgb[2] > 100) ? 255 : 0;
            }

            color = RGBToColor(rgb);

            double sumWhiteScale = Math.sqrt(Math.pow(sumWhiteX,2) + Math.pow(sumWhiteY,2));
//            Log.d("thaw-sumwhite","X:" + sumWhiteX);
//            Log.d("thaw-sumwhite","Y:" + sumWhiteY);
//            Log.d("thaw-sumwhite","Scale:" + sumWhiteScale);

            if (sumWhiteScale == 0) {
                sumWhiteX = 128;
                sumWhiteY = 128;
            } else {
                sumWhiteX = (int) (((sumWhiteX/sumWhiteScale) * 128) + 128);
                sumWhiteY = (int) (((sumWhiteY/sumWhiteScale) * 128) + 128);
            }
//            double whiteangle = Math.toDegrees(Math.atan2(sumWhiteY-128,sumWhiteX-128));
//            Log.d("thaw-sumwhite", "Deg:" + whiteangle);
//            Log.d("thaw-sumwhite", "X:" + sumWhiteX);
//            Log.d("thaw-sumwhite", "Y:" + sumWhiteY);

            colorToRGB(color, rgb);
            udpout[0] = (byte) rgb[0];
            udpout[1] = (byte) rgb[1];
            udpout[2] = (byte) rgb[2];

            udpout[5] = (byte) sumWhiteX;
            udpout[6] = (byte) sumWhiteY;
            udpout[7] = (byte) count;
//            udpout[8] = (byte) state;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("thaw-calc-color", "can't get bitmap");
        }finally {
            return color;
        }
    }

    private void colorToRGB(int color, int[] rgb){
        rgb[0] = (color & 0x00FF0000) >> 16;
        rgb[1] = (color & 0x0000FF00) >> 8;
        rgb[2] = (color & 0x000000FF) >> 0;
    }

    private int RGBToColor(int[] rgb){
        int r = (rgb[0] << 16);
        int g = (rgb[1] << 8);
        int b = (rgb[2] << 0);
        int out = r | g | b | 0xFF000000;
        return out;
    }

    class PixelDrawer{
        private Canvas canvas;
        private Paint paint;
        private TextureView textureView;

        public void init(TextureView textureView){
            this.textureView = textureView;
            try {
                canvas = textureView.lockCanvas();
                canvas.drawColor(0xff000000, PorterDuff.Mode.CLEAR);
                paint = new Paint();
            }catch(Exception e) {}
        }

        public void draw(int x,int y,int color){
            try {
                paint.setColor(color);
                canvas.drawRect(
                        x-pixelSize, y-pixelSize,
                        x+pixelSize, y+pixelSize, paint);
            }catch(Exception e) {}
        }

        public void post(){
            try {
                textureView.unlockCanvasAndPost(canvas);
            }catch(Exception e) {}
        }
    }
}
