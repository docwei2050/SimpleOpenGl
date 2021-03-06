package com.docwei.simpleopengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by liwk on 2021/2/15.
 */
public class SimpleTextureRender implements SimpleGlRender {
    private Context context;
    //顶点坐标
    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1, 1f,
            1f, 1f,
            //第二张图片的位置
            -0.5f, -0.5f,
            0.5f, -0.5f,
            -0.5f, 0.5f,
            0.5f, 0.5f
    };
    private FloatBuffer vertextBuffer;
    private float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    private FloatBuffer fragmentBuffer;
    private int program;
    private int vPosition;
    private int fPosition;
    private int textureId;
    private int sampler;
    private int vboId;
    private int fBOId;
    private int imageTextureId;
    private int imageTextureId2;
    private FboRender mFboRender;
    private int umatrix;
    private float[] matrix = new float[16];
    private int width;
    private int height;

    public SimpleTextureRender(Context context) {
        this.context = context;
        mFboRender = new FboRender(context);
        vertextBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                 .asFloatBuffer().put(vertexData);
        vertextBuffer.position(0);

        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);

    }
    private OnTextureIdReadyListener mOnTextureIdReadyListener;

    public void setOnTextureIdReadyListener(OnTextureIdReadyListener onTextureIdReadyListener) {
        mOnTextureIdReadyListener = onTextureIdReadyListener;
    }

    @Override
    public void onSurfaceCreated() {
        mFboRender.onCreate();
        String vertexSource = ShaderUtil.readRawTxt(context, R.raw.vertex_shader_m);
        String fragmentSource = ShaderUtil.readRawTxt(context, R.raw.fragment_shader);
        program = ShaderUtil.createProgram(vertexSource, fragmentSource);
        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");
        sampler = GLES20.glGetUniformLocation(program, "sTexture");
        umatrix = GLES20.glGetUniformLocation(program, "u_Matrix");


        //创建vbo
        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];
        //绑定和解绑vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertextBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        //创建Fbo
        int[] fbos = new int[1];
        GLES20.glGenBuffers(1, fbos, 0);
        fBOId = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fBOId);
        //生成纹理
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        textureId=textureIds[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(sampler, 0);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1170, 1290, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("simpleGl", "fbo error");
        } else {
            Log.e("simpleGl", "fbo success");
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        // imageTextureId = loadTexture(R.mipmap.a);
        imageTextureId = loadTexture(R.mipmap.a);
        imageTextureId2 = loadTexture(R.mipmap.b);
        if (mOnTextureIdReadyListener != null) {
            //这个就是要共享的纹理 这个在后面会被搞成离屏缓冲的纹理
            mOnTextureIdReadyListener.success(textureId);
        }

    }

    private int loadTexture(int src) {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), src);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureIds[0];
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        this.width=width;
        this.height=height;

        width = 1170;
        height = 1290;


        Log.e("simpleGl","widht--."+width+"---"+height);
        mFboRender.onChange(width, height);
        
//        if (width > height) {
//            Matrix.orthoM(matrix, 0, -width / ((height / 1280f) * 720f), width / ((height / 1280f) * 720f), -1, 1f, -1f, 1f);
//        } else {
//            Matrix.orthoM(matrix, 0, -1,1, -height / ((width / 720f) * 1280f), height / ((width / 720f) * 1280f), -1f, 1f);
//        }
        Matrix.orthoM(matrix, 0, -width / ((height / 1280f) * 720f), width / ((height / 1280f) * 720f), -1, 1f, -1f, 1f);
        Matrix.rotateM(matrix, 0, 180, 1, 0, 0);

    }

    @Override
    public void onDrawFrame() {
        GLES20.glViewport(0, 0, 1170, 1290);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fBOId);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(umatrix, 1, false, matrix, 0);


        //绑定vbo以便能使用顶点坐标和纹理坐标
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);


        GLES20.glEnableVertexAttribArray(vPosition);
        //从vbo取   //绘制第一张图片
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTextureId);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        //从vbo取   //绘制第二张图片     32 = 4个顶点*8
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTextureId2);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 32);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);



        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
       // GLES20.glViewport(0, 0, width, height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        mFboRender.onDraw(textureId);
    }
    public  interface OnTextureIdReadyListener{
        void success(int textureId);
    }
}
