package com.rockchip.gpadc.ssddemo;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

import static com.rockchip.gpadc.ssddemo.CameraSurfaceRender.TAG;
import com.rockchip.gpadc.ssddemo.InferenceResult.Recognition;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Randall on 2018/10/15
 */
public class MainActivity extends Activity {

    private GLSurfaceView mGLSurfaceView;
    private CameraSurfaceRender mRender;
    private TextView mFpsNum1;
    private TextView mFpsNum2;
    private TextView mFpsNum3;
    private TextView mFpsNum4;
    private ImageView mTrackResultView;
    private Bitmap mTrackResultBitmap = null;
    private Canvas mTrackResultCanvas = null;
    private Paint mTrackResultPaint = null;
    private Paint mTrackResultTextPaint = null;

    private PorterDuffXfermode mPorterDuffXfermodeClear;
    private PorterDuffXfermode mPorterDuffXfermodeSRC;

    Call<CareModel> call;
    private int countRegoPerson = 0;

    // UI线程，用于更新处理结果
    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if (msg.what == 0) {
                float fps = (float) msg.obj;

                DecimalFormat decimalFormat = new DecimalFormat("00.00");
                String fpsStr = decimalFormat.format(fps);
                mFpsNum1.setText(String.valueOf(fpsStr.charAt(0)));
                mFpsNum2.setText(String.valueOf(fpsStr.charAt(1)));
                mFpsNum3.setText(String.valueOf(fpsStr.charAt(3)));
                mFpsNum4.setText(String.valueOf(fpsStr.charAt(4)));
            } else {
                showTrackSelectResults();
//                Log.d(TAG, "msg.what:" + msg.toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFpsNum1 = (TextView) findViewById(R.id.fps_num1);
        mFpsNum2 = (TextView) findViewById(R.id.fps_num2);
        mFpsNum3 = (TextView) findViewById(R.id.fps_num3);
        mFpsNum4 = (TextView) findViewById(R.id.fps_num4);
        mTrackResultView = (ImageView) findViewById(R.id.canvasView);

        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mRender = new CameraSurfaceRender(mGLSurfaceView, mHandler);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        Log.d(TAG, "CareClient getCareService");
        call = CareClient.getCareService().doGetHomeMsg();;
        call.enqueue(new Callback<CareModel>(){
            @Override
            public void onResponse(Call<CareModel> call, Response<CareModel> response) {
                CareModel result = response.body();
                Log.d(TAG, "CareClient Resp.:" + result.getMsg());
            }

            @Override
            public void onFailure(Call<CareModel> call, Throwable t) {
                Log.d(TAG, "CareClient Resp. failed:" + t.toString());
            }
        });
    }

    public static int sp2px(float spValue) {
        Resources r = Resources.getSystem();
        final float scale = r.getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }

    private void showTrackSelectResults() {

        //todo
        // if we use mRender's resolution, draw paint is slow.
        int width = 640; //mRender.getWidth()
        int height = 480; //mRender.getHeight()

        if (mTrackResultBitmap == null) {

            mTrackResultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mTrackResultCanvas = new Canvas(mTrackResultBitmap);

            //用于画线
            mTrackResultPaint = new Paint();
            mTrackResultPaint.setColor(0xff06ebff);
            mTrackResultPaint.setStrokeJoin(Paint.Join.ROUND);
            mTrackResultPaint.setStrokeCap(Paint.Cap.ROUND);
            mTrackResultPaint.setStrokeWidth(4);
            mTrackResultPaint.setStyle(Paint.Style.STROKE);
            mTrackResultPaint.setTextAlign(Paint.Align.LEFT);
            mTrackResultPaint.setTextSize(sp2px(10));
            mTrackResultPaint.setTypeface(Typeface.SANS_SERIF);
            mTrackResultPaint.setFakeBoldText(false);

            //用于文字
            mTrackResultTextPaint = new Paint();
            mTrackResultTextPaint.setColor(0xff06ebff);
            mTrackResultTextPaint.setStrokeWidth(2);
            mTrackResultTextPaint.setTextAlign(Paint.Align.LEFT);
            mTrackResultTextPaint.setTextSize(sp2px(12));
            mTrackResultTextPaint.setTypeface(Typeface.SANS_SERIF);
            mTrackResultTextPaint.setFakeBoldText(false);


            mPorterDuffXfermodeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            mPorterDuffXfermodeSRC = new PorterDuffXfermode(PorterDuff.Mode.SRC);
        }

        // clear canvas
        mTrackResultPaint.setXfermode(mPorterDuffXfermodeClear);
        mTrackResultCanvas.drawPaint(mTrackResultPaint);
        mTrackResultPaint.setXfermode(mPorterDuffXfermodeSRC);

        //detect result
        // todo, mRender.getTrackResult() is slow, you can move it to a new thread
        ArrayList<Recognition> recognitions = mRender.getTrackResult();
        for (int i=0; i<recognitions.size(); ++i) {
            InferenceResult.Recognition rego = recognitions.get(i);
            RectF detection = rego.getLocation();

            detection.left *= width;
            detection.right *= width;
            detection.top *= height;
            detection.bottom *= height;

//            Log.d(TAG, "rego:" + rego.toString() + " t:" + rego.getTitle() + " cls:" + rego.getClass());

            mTrackResultCanvas.drawRect(detection, mTrackResultPaint);
            mTrackResultCanvas.drawText(rego.getTitle(), detection.left+5, detection.bottom-5, mTrackResultTextPaint);

            if (Objects.equals(rego.getTitle(), "person")) {
                countRegoPerson++;
//                Log.d(TAG, "regoPersonCount increase..." + Integer.toString(countRegoPerson));
            }
        }

        mTrackResultView.setScaleType(ImageView.ScaleType.FIT_XY);
        mTrackResultView.setImageBitmap(mTrackResultBitmap);

        if (countRegoPerson > 100) {  // continuous recog. for person per 5sec.
            countRegoPerson = 0;

            Log.d(TAG, "CareClient getCareService");
            CareModel careModel = new CareModel();
            careModel.setMsg("emergency");
            call = CareClient.getCareService().doPostEmergency(careModel);
            call.enqueue(new Callback<CareModel>(){
                @Override
                public void onResponse(Call<CareModel> call, Response<CareModel> response) {
                    CareModel result = response.body();
                    Log.d(TAG, "CareClient Resp.:" + result.getMsg());
                }

                @Override
                public void onFailure(Call<CareModel> call, Throwable t) {
                    Log.d(TAG, "CareClient Resp. failed:" + t.toString());
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRender.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRender.onResume();
        mGLSurfaceView.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
}
