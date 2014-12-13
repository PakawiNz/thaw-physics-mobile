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
    private final int stepSize = 30;
    private int[] rgb = new int[3];

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

            // double maxrange = Math.sqrt(Math.pow(spanSize/2,2) + Math.pow(spanSize/2,2));

            pixelDrawer.init(fragment.getUpperTextureView());
            for (int i = cx-spanSize; i <= cx+spanSize; i += stepSize){
                for (int j = cy-spanSize; j <= cy+spanSize; j += stepSize){
                    color = bitmap.getPixel(i, j);
                    colorToRGBint(color, rgb);
                    // adj = maxrange - Math.sqrt(Math.pow(i-cx,2) + Math.pow(j-cy,2));
                    // adj /= maxrange;
                    sumR += rgb[0]*adj;
                    sumG += rgb[1]*adj;
                    sumB += rgb[2]*adj;
                    count += adj;

                    if (rgb[0] > 140 && rgb[1] > 140 && rgb[2] > 140) {
//                        Log.d("rgb-color",String.format("%03d %03d %03d",rgb[0],rgb[1],rgb[2]));
//                        Log.d("thaw-sumwhite","X:" + sumWhiteX);
                        sumWhiteX += i - cx;
                        sumWhiteY += j - cy;
                        whitePixelCount++;

                        pixelDrawer.draw(i,j,Color.WHITE);
                    }else {
                        pixelDrawer.draw(i,j,color);
                    }
                }
            }
            pixelDrawer.post();

            int pixelAmount = spanSize/stepSize * spanSize/stepSize * 4;
            int state = 0;
            if (0.75 * pixelAmount < whitePixelCount) {
                state = 2;
            } else if (0.25 * pixelAmount < whitePixelCount) {
                state = 1;
            }

            rgb[0] = (int)(sumR / count);
            rgb[1] = (int)(sumG / count);
            rgb[2] = (int)(sumB / count);

            color = RGBToColor(rgb);

            double sumWhiteScale = Math.sqrt(Math.pow(sumWhiteX,2) + Math.pow(sumWhiteY,2));
//            Log.d("thaw-sumwhite","X:" + sumWhiteX);
//            Log.d("thaw-sumwhite","Y:" + sumWhiteY);
//            Log.d("thaw-sumwhite","Scale:" + sumWhiteScale);
            if (sumWhiteScale == 0) {
                udpout[5] = 0;
                udpout[6] = 0;
            } else {
                Log.d("thaw-sumwhite", "X:" + ((sumWhiteX / sumWhiteScale) * 128));
                Log.d("thaw-sumwhite", "Y:" + ((sumWhiteY / sumWhiteScale) * 128));
                udpout[5] = (byte) (((sumWhiteX/sumWhiteScale) * 128) + 128);
                udpout[6] = (byte) (((sumWhiteY/sumWhiteScale) * 128) + 128);
            }
            colorToRGB(color,udpout);
            udpout[7] = (byte) pixelAmount;
            udpout[8] = (byte) state;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("thaw-calc-color", "can't get bitmap");
        }finally {
            return color;
        }
    }

    private void colorToRGB(int color,byte[] rgb){
        rgb[0] = (byte)((color & 0x00FF0000) >> 16);
        rgb[1] = (byte)((color & 0x0000FF00) >> 8);
        rgb[2] = (byte)((color & 0x000000FF) >> 0);
    }

    private void colorToRGBint(int color,int[] rgb){
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

    private final int pixelSize = 10;
    class PixelDrawer{
        private Canvas canvas;
        private Paint paint;
        private TextureView textureView;

        public void init(TextureView textureView){
            try {
                this.textureView = textureView;
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
                fragment.getUpperTextureView().unlockCanvasAndPost(canvas);
            }catch(Exception e) {}
        }
    }
}
