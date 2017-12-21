package com.devlin_n.videoplayer.player;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.devlin_n.videoplayer.R;
import com.devlin_n.videoplayer.controller.BaseVideoController;
import com.devlin_n.videoplayer.util.Constants;
import com.devlin_n.videoplayer.util.NetworkUtil;
import com.devlin_n.videoplayer.util.WindowUtil;
import com.devlin_n.videoplayer.widget.ResizeSurfaceView;
import com.devlin_n.videoplayer.widget.ResizeTextureView;
import com.devlin_n.videoplayer.widget.StatusView;

import java.util.List;

import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 播放器
 * Created by Devlin_n on 2017/4/7.
 */

public class IjkVideoView extends BaseIjkVideoView {

    @Nullable
    private BaseVideoController mVideoController;//控制器
    private ResizeSurfaceView mSurfaceView;
    private ResizeTextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private DanmakuView mDanmakuView;
    private DanmakuContext mContext;
    private BaseDanmakuParser mParser;
    private FrameLayout playerContainer;
    private StatusView statusView;//显示错误信息的一个view
    private boolean useSurfaceView;//是否使用TextureView
    private boolean isFullScreen;//是否处于全屏状态

    public static final int SCREEN_SCALE_DEFAULT = 0;
    public static final int SCREEN_SCALE_16_9 = 1;
    public static final int SCREEN_SCALE_4_3 = 2;
    public static final int SCREEN_SCALE_MATCH_PARENT = 3;
    public static final int SCREEN_SCALE_ORIGINAL = 4;

    private int mCurrentScreenScale = SCREEN_SCALE_DEFAULT;

    public IjkVideoView(@NonNull Context context) {
        this(context, null);
    }

    public IjkVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IjkVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }


    /**
     * 初始化播放器视图
     */
    protected void initView() {
        Constants.SCREEN_HEIGHT = WindowUtil.getScreenHeight(getContext(), false);
        Constants.SCREEN_WIDTH = WindowUtil.getScreenWidth(getContext());
        playerContainer = new FrameLayout(getContext());
        playerContainer.setBackgroundColor(Color.BLACK);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(playerContainer, params);
    }

    /**
     * 创建播放器实例，设置播放地址及播放器参数
     */
    @Override
    protected void initPlayer() {
        super.initPlayer();
        addDisplay();
        if (mDanmakuView != null) {
            playerContainer.removeView(mDanmakuView);
            playerContainer.addView(mDanmakuView, 1);
        }
    }

    private void addDisplay() {
        if (useSurfaceView) {
            addSurfaceView();
        } else {
            addTextureView();
        }
    }

    @Override
    protected void setPlayState(int playState) {
        if (mVideoController != null) mVideoController.setPlayState(playState);
    }

    @Override
    protected void setPlayerState(int playerState) {
        if (mVideoController != null) mVideoController.setPlayerState(playerState);
    }

    @Override
    protected void startPlay() {
        super.startPlay();
        if (addToPlayerManager) {
            VideoViewManager.instance().releaseVideoPlayer();
            VideoViewManager.instance().setCurrentVideoPlayer(this);
        }
    }

    @Override
    protected void startPrepare() {
        super.startPrepare();
        if (mDanmakuView != null) {
            mDanmakuView.prepare(mParser, mContext);
        }
    }
    /**
     * 添加SurfaceView
     */
    private void addSurfaceView() {
        playerContainer.removeView(mSurfaceView);
        mSurfaceView = new ResizeSurfaceView(getContext());
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(holder);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        playerContainer.addView(mSurfaceView, 0, params);
    }

    /**
     * 添加TextureView
     */
    private void addTextureView() {
        playerContainer.removeView(mTextureView);
        mSurfaceTexture = null;
        mTextureView = new ResizeTextureView(getContext());
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                if (mSurfaceTexture != null) {
                    mTextureView.setSurfaceTexture(mSurfaceTexture);
                } else {
                    mSurfaceTexture = surfaceTexture;
                    mMediaPlayer.setSurface(new Surface(surfaceTexture));
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return mSurfaceTexture == null;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        playerContainer.addView(mTextureView, 0, params);
    }


    @Override
    protected boolean checkNetwork() {
        if (NetworkUtil.getNetworkType(getContext()) == NetworkUtil.NETWORK_MOBILE && !Constants.IS_PLAY_ON_MOBILE_NETWORK) {
            playerContainer.removeView(statusView);
            if (statusView == null) {
                statusView = new StatusView(getContext());
            }
            statusView.setMessage(getResources().getString(R.string.wifi_tip));
            statusView.setButtonTextAndAction(getResources().getString(R.string.continue_play), v -> {
                Constants.IS_PLAY_ON_MOBILE_NETWORK = true;
                playerContainer.removeView(statusView);
                initPlayer();
                startPrepare();
            });
            playerContainer.addView(statusView);
            return true;
        }
        return false;
    }

    @Override
    protected void startInPlaybackState() {
        super.startInPlaybackState();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    @Override
    public void pause() {
        super.pause();
        if (isInPlaybackState()) {
            if (mDanmakuView != null && mDanmakuView.isPrepared()) {
                mDanmakuView.pause();
            }
        }
    }

    @Override
    public void resume() {
        super.resume();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }


    @Override
    public void release() {
        super.release();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
        playerContainer.removeView(mTextureView);
        playerContainer.removeView(mSurfaceView);
        playerContainer.removeView(statusView);
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    @Override
    public void seekTo(int pos) {
        super.seekTo(pos);
        if (isInPlaybackState()) {
            if (mDanmakuView != null) mDanmakuView.seekTo((long) pos);
        }
    }

    @Override
    public void startFullScreen() {
        if (isFullScreen) return;
        WindowUtil.hideSystemBar(getContext());
        this.removeView(playerContainer);
        ViewGroup contentView = WindowUtil.scanForActivity(getContext())
                .findViewById(android.R.id.content);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(playerContainer, params);
        isFullScreen = true;
        if (mVideoController != null) mVideoController.setPlayerState(PLAYER_FULL_SCREEN);
    }

    @Override
    public void stopFullScreen() {
        if (!isFullScreen) return;
        WindowUtil.showSystemBar(getContext());
        ViewGroup contentView = WindowUtil.scanForActivity(getContext())
                .findViewById(android.R.id.content);
        contentView.removeView(playerContainer);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(playerContainer, params);
        isFullScreen = false;
        if (mVideoController != null) mVideoController.setPlayerState(PLAYER_NORMAL);
    }

    @Override
    public boolean isFullScreen() {
        return isFullScreen;
    }

    @Override
    public void onError() {
        super.onError();
        playerContainer.removeView(statusView);
        if (statusView == null) {
            statusView = new StatusView(getContext());
        }
        statusView.setMessage(getResources().getString(R.string.error_message));
        statusView.setButtonTextAndAction(getResources().getString(R.string.retry), v -> {
            playerContainer.removeView(statusView);
            mMediaPlayer.reset();
            startPrepare();
        });
        playerContainer.addView(statusView);
    }

    @Override
    public void onInfo(int what, int extra) {
        super.onInfo(what, extra);
        switch (what) {
            case IjkMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                if (mTextureView != null)
                    mTextureView.setRotation(extra);
                break;
        }
    }

    @Override
    public void onVideoSizeChanged(int videoWidth, int videoHeight) {
        if (useSurfaceView) {
            mSurfaceView.setScreenScale(mCurrentScreenScale);
            mSurfaceView.setVideoSize(videoWidth, videoHeight);
        } else {
            mTextureView.setScreenScale(mCurrentScreenScale);
            mTextureView.setVideoSize(videoWidth, videoHeight);
        }
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        mCurrentVideoPosition++;
        if (mVideoModels != null && mVideoModels.size() > 1) {
            if (mCurrentVideoPosition >= mVideoModels.size()) {
                return;
            }
            playNext();
            mMediaPlayer.reset();
            addDisplay();
            startPrepare();
        }
    }



    /**
     * 设置控制器
     */
    public IjkVideoView setVideoController(@Nullable BaseVideoController mediaController) {
        playerContainer.removeView(mVideoController);
        if (mediaController != null) {
            mediaController.setMediaPlayer(this);
            mVideoController = mediaController;
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            playerContainer.addView(mVideoController, params);
        }
        return this;
    }

    /**
     * 播放下一条视频，可用于跳过广告
     */
    @Override
    public void skipToNext() {
        mCurrentVideoPosition++;
        if (mVideoModels != null && mVideoModels.size() > 1) {
            if (mCurrentVideoPosition >= mVideoModels.size()) return;
            playNext();
            mMediaPlayer.reset();
            addDisplay();
            startPrepare();
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    public boolean onBackPressed() {
        return mVideoController != null && mVideoController.onBackPressed();
    }

    /**
     * 设置视频地址
     */
    public IjkVideoView setUrl(String url) {
        this.mCurrentUrl = url;
        return this;
    }

    /**
     * 一开始播放就seek到预先设置好的位置
     */
    public IjkVideoView skipPositionWhenPlay(String url, int position) {
        this.mCurrentUrl = url;
        this.mCurrentPosition = position;
        return this;
    }

    /**
     * 设置一个列表的视频
     */
    public IjkVideoView setVideos(List<VideoModel> videoModels) {
        this.mVideoModels = videoModels;
        playNext();
        return this;
    }

    /**
     * 设置标题
     */
    public IjkVideoView setTitle(String title) {
        if (title != null) {
            this.mCurrentTitle = title;
        }
        return this;
    }

    /**
     * 开启缓存
     */
    public IjkVideoView enableCache() {
        isCache = true;
        return this;
    }

    /**
     * 添加到{@link VideoViewManager},如需集成到RecyclerView或ListView请开启此选项
     */
    public IjkVideoView addToPlayerManager() {
        addToPlayerManager = true;
        return this;
    }

    /**
     * 播放下一条视频
     */
    private void playNext() {
        VideoModel videoModel = mVideoModels.get(mCurrentVideoPosition);
        if (videoModel != null) {
            mCurrentUrl = videoModel.url;
            mCurrentTitle = videoModel.title;
            mCurrentPosition = 0;
            setVideoController(videoModel.controller);
        }
    }


    /**
     * 添加弹幕
     */
    public IjkVideoView addDanmukuView(DanmakuView danmakuView, DanmakuContext context, BaseDanmakuParser parser) {
        this.mDanmakuView = danmakuView;
        this.mContext = context;
        this.mParser = parser;
        return this;
    }


    /**
     * 设置视频比例
     */
    @Override
    public IjkVideoView setScreenScale(int screenScale) {
        this.mCurrentScreenScale = screenScale;
        if (mSurfaceView != null) mSurfaceView.setScreenScale(screenScale);
        if (mTextureView != null) mTextureView.setScreenScale(screenScale);
        return this;
    }

    /**
     * 启用SurfaceView
     */
    public IjkVideoView useSurfaceView() {
        this.useSurfaceView = true;
        return this;
    }

    /**
     * 启用{@link android.media.MediaPlayer},如不调用默认使用 {@link IjkMediaPlayer}
     */
    public IjkVideoView useAndroidMediaPlayer() {
        this.useAndroidMediaPlayer = true;
        return this;
    }

    /**
     * 锁定全屏播放
     */
    public IjkVideoView alwaysFullScreen() {
        mAlwaysFullScreen = true;
        return this;
    }

    /**
     * 设置自动旋转
     */
    public IjkVideoView autoRotate() {
        this.mAutoRotate = true;
        if (orientationEventListener == null) {
            orientationEventListener = new OrientationEventListener(getContext()) { // 加速度传感器监听，用于自动旋转屏幕

                private int CurrentOrientation = 0;
                private static final int PORTRAIT = 1;
                private static final int LANDSCAPE = 2;
                private static final int REVERSE_LANDSCAPE = 3;

                @Override
                public void onOrientationChanged(int orientation) {
                    if (orientation >= 340) { //屏幕顶部朝上
                        if (isLocked || mAlwaysFullScreen) return;
                        if (CurrentOrientation == PORTRAIT) return;
                        if ((CurrentOrientation == LANDSCAPE || CurrentOrientation == REVERSE_LANDSCAPE) && !isFullScreen()) {
                            CurrentOrientation = PORTRAIT;
                            return;
                        }
                        CurrentOrientation = PORTRAIT;
                        WindowUtil.scanForActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        stopFullScreen();
                    } else if (orientation >= 260 && orientation <= 280) { //屏幕左边朝上
                        if (CurrentOrientation == LANDSCAPE) return;
                        if (CurrentOrientation == PORTRAIT && isFullScreen()) {
                            CurrentOrientation = LANDSCAPE;
                            return;
                        }
                        CurrentOrientation = LANDSCAPE;
                        if (!isFullScreen()) {
                            startFullScreen();
                        }
                        WindowUtil.scanForActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else if (orientation >= 70 && orientation <= 90) { //屏幕右边朝上
                        if (CurrentOrientation == REVERSE_LANDSCAPE) return;
                        if (CurrentOrientation == PORTRAIT && isFullScreen()) {
                            CurrentOrientation = REVERSE_LANDSCAPE;
                            return;
                        }
                        CurrentOrientation = REVERSE_LANDSCAPE;
                        if (!isFullScreen()) {
                            startFullScreen();
                        }
                        WindowUtil.scanForActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    }
                }
            };
        }
        return this;
    }
}
