package cn.ittiger.player;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.commonutil.KeyMsgEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import cn.ittiger.player.message.DurationMessage;
import cn.ittiger.player.message.Message;
import cn.ittiger.player.message.UIStateMessage;
import cn.ittiger.player.state.PlayState;
import cn.ittiger.player.state.ScreenState;
import cn.ittiger.player.util.Utils;
import cn.ittiger.player.view.BatteryView;
import cn.ittiger.player.view.DigitalClock;
import cn.ittiger.player.view.IjkVideoContract;
import cn.ittiger.player.view.NetStatusView;

/**
 * Created by kiven on 2/1/18.
 */

public class IjkVideoView extends FrameLayout implements
        View.OnClickListener,
        View.OnTouchListener,
        SeekBar.OnSeekBarChangeListener,
        AudioManager.OnAudioFocusChangeListener,
        IjkVideoContract.IVideoView,
        Observer{

    @BindView(R2.id.surface_container)
    protected ViewGroup mTextureViewContainer; //渲染控件父类

    protected View mSmallClose; //小窗口关闭按键

    protected Map<String, String> mMapHeadData = new HashMap<>();

    protected TextureView mTextureView;

    @BindView(R2.id.btn_start)
    ImageView mStartButton;

    @BindView(R2.id.video_prop_window)
    RelativeLayout mPopView;

    @BindView(R2.id.video_time_ctrl_view)
    LinearLayout mPopTimeCtrlView;

    @BindView(R2.id.video_pop_icon)
    ImageView mPopIconView;

    @BindView(R2.id.video_pop_content)
    FrameLayout mPopContentView;

    @BindView(R2.id.tv_current)
    TextView mPopCurTimeView;

    @BindView(R2.id.tv_duration)
    protected TextView mPopTotalTimeView;

    @BindView(R2.id.video_pop_progress)
    protected ProgressBar mPopPregressBar;

    @BindView(R2.id.bottom_seekbar)
    protected SeekBar mBottomSeekBar;

    @BindView(R2.id.bottom_progressbar)
    protected ProgressBar mBottomProgressBar;

    @BindView(R2.id.btn_screen_rotate)
    protected ImageView mFullscreenButton;

    @BindView(R2.id.bottom_tv_current)
    protected TextView mBottomCurTimeView;

    @BindView(R2.id.bottom_tv_total)
    protected TextView mTotalTimeTextView;

    @BindView(R2.id.loading)
    RelativeLayout mLoadingView;

    @BindView(R2.id.layout_top)
    ViewGroup mTopContainer;

    @BindView(R2.id.layout_bottom)
    ViewGroup mBottomContainer;

    @BindView(R2.id.vp_video_thumb)
    ImageView mVideoThumbView;

    @BindView(R2.id.system_status_view)
    protected ViewGroup mTopStatusView;

    @BindView(R2.id.data)
    protected NetStatusView mWifiView;

    @BindView(R2.id.battery)
    protected BatteryView mBatteryView;

    @BindView(R2.id.time)
    protected DigitalClock mTimeView;

    @BindView(R2.id.btn_back)
    protected ImageView mBackButton;

    @BindView(R2.id.net_error)
    protected ViewGroup mNetView;

    @BindView(R2.id.net_error_again)
    protected TextView mRetryBtn; // retry btn when net error.

    @BindView(R2.id.video_replay_view)
    protected ViewGroup mReplayView;

    @BindView(R2.id.video_tv_replay)
    protected TextView mReplayBtn;

    @BindView(R2.id.btn_lock)
    protected ImageView mLockBtn;

    @BindView(R2.id.video_cover_img)
    protected ImageView mCoverView; // video cover img.

    @BindView(R2.id.tv_title)
    protected TextView mTitleTextView; //title

    @BindView(R2.id.player_buffer)
    TextView mBufferTextView; // 网络缓冲速度

    protected Bitmap mFullPauseBitmap = null;//暂停时的全屏图片；

    protected float mBrightnessData;
    /**
     * 视频时长，miliseconds
     */
    private int mDuration = 0;
    /**
     * 当前Observer（即：VideoPlayerView本身）对象的hashcode
     */
    private int mViewHash;

    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * 屏幕高度
     */
    private int mScreenHight;
    /**
     * 小窗口的宽度
     */
    private int mSmallWindowWidth;
    /**
     * 小窗口的高度
     */
    private int mSmallWindowHeight;

    /**
     * 视频标题
     */
    private CharSequence mVideoTitle;
    /**
     * 视频地址
     */
    private String mVideoUrl;


    protected int mCurrentState = -1; //当前的播放状态
    /**
     * 当前屏幕播放状态
     */
    private int mCurrentScreenState = ScreenState.SCREEN_STATE_NORMAL;

    private VideoPresenter mPresenter;

    /**
     * 正常状态下的标题是否显示
     */
    private boolean mShowNormalStateTitleView = true;
    private Handler mHandler;



    /************************ 全屏播放相关操作 ********************************/
    /**
     * 切换全屏播放状态
     */
    private boolean mToggleFullScreen = false;
    /**
     * 切换全屏播放前当前VideoPlayerView的父容器
     */
    private ViewGroup mOldParent;
    /**
     * 切换全屏播放前当前VideoPlayerView的在父容器中的索引
     */
    private int mOldIndex = 0;
    /**
     * 切换到全屏播放前当前VideoPlayerView的宽度
     */
    private int mVideoWidth;
    /**
     * 切换到全屏播放前当前VideoPlayerView的高度
     */
    private int mVideoHeight;

    public IjkVideoView(@NonNull Context context) {
        super(context);
        initView(context);
        initData(context);
    }


    public IjkVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initData(context);
    }

    public IjkVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        initData(context);
    }



    @Override
    public void onAudioFocusChange(int focusChange) {

    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mToggleFullScreen = false;
        PlayerManager.getInstance().addObserver(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PlayerManager.getInstance().removeObserver(this);
        EventBus.getDefault().unregister(this);
        if (mToggleFullScreen) {
            return;
        }
        if(mCurrentState != PlayState.STATE_NORMAL) {
            PlayerManager.getInstance().stop();
            onPlayStateChanged(PlayState.STATE_NORMAL);
        }

    }

    private void initView(Context context) {

        View videoView = LayoutInflater.from(context).inflate(R.layout.layout_ijk_video, this);
        ButterKnife.bind(this, videoView);
        //避免ListView中item点击无法响应的问题
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setBackgroundColor(Color.BLACK);
        videoView.setFocusable(true);
        mPresenter = new VideoPresenter(this);
    }

    private void initData(Context context) {
        mViewHash = this.toString().hashCode();
        mSmallWindowWidth = mScreenWidth / 2;
        mSmallWindowHeight = (int) (mSmallWindowWidth * 1.0f / 16 * 9 + 0.5f);
        mHandler = new ProgressHandler(this);
        mBottomSeekBar.setOnSeekBarChangeListener(this);
    }

    /**
     * 绑定数据
     * @param videoUrl
     */
    public void bind(String videoUrl, CharSequence title) {
        bind(videoUrl, title, mShowNormalStateTitleView);
        resetViewState();
    }

    private void resetViewState() {

        mCurrentState = PlayState.STATE_NORMAL;
        mCurrentScreenState = ScreenState.SCREEN_STATE_NORMAL;
        onPlayStateChanged(mCurrentState);
    }

    /**
     * 绑定数据
     * @param videoUrl
     */
    public void bind(String videoUrl, CharSequence title, boolean showNormalStateTitleView) {
        mShowNormalStateTitleView = showNormalStateTitleView;
        mVideoTitle = title;
        mVideoUrl = videoUrl;
        if(!TextUtils.isEmpty(mVideoTitle)) {
            mTitleTextView.setText(mVideoTitle);
        }
        resetViewState();
    }


    @Override
    public void onClick(View v) {

    }

    @OnClick(R2.id.surface_container)
    void clickContainer() {
        if(!PlayerManager.getInstance().isViewPlaying(mViewHash)) {
            //存在正在播放的视频，先将上一个视频停止播放，再继续下一个视频的操作
            PlayerManager.getInstance().stop();
        }
        mPresenter.handleVideoContainerLogic(mCurrentState, mBottomContainer.getVisibility() ==VISIBLE);
    }


    @OnClick(R2.id.btn_start)
    void clickStartBtn() {
        mPresenter.handleStartLogic(mViewHash, mVideoUrl, mCurrentState);
    }


    @OnClick(R2.id.btn_screen_rotate)
    void clickRotateBtn() {
        Activity activity = (Activity) getContext();
        mPresenter.handleScreenRotate(activity.getRequestedOrientation());
    }

    @OnClick(R2.id.btn_lock)
    void clickLockBtn() {
        mPresenter.handleLockLogic();
    }

    @OnClick(R2.id.btn_back)
    void clickBackBtn() {
        changeUINormalScreen();
    }



    @OnTouch(R2.id.surface_container)
    boolean onTouchContainer(MotionEvent event) {
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHight = getContext().getResources().getDisplayMetrics().heightPixels;

        return mPresenter.handleContainerTouchLogic(mCurrentState, event, mScreenWidth, mScreenHight);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser && seekBar != null) {
            int seekToTime = seekBar.getProgress() * mDuration / 100;
            PlayerManager.getInstance().seekTo(seekToTime);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void update(Observable o, final Object arg) {
        //判断当前layout是否销毁
        if(getContext() == null) {
            return;
        }
        //判断当前是否是
        if(!(arg instanceof Message)) {
            return;
        }

        if(mViewHash != ((Message) arg).getHash() ||
                !mVideoUrl.equals(((Message) arg).getVideoUrl())) {
            return;
        }

        if(arg instanceof DurationMessage) {
            ((Activity)getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    onDurationChanged(((DurationMessage) arg).getDuration());
                }
            });
            return;
        }

        if(!(arg instanceof UIStateMessage)) {
            return;
        }
        ((Activity)getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                onPlayStateChanged(((UIStateMessage) arg).getState());
            }
        });


    }

    /**
     * 更新视频的时长并自动更新视频进度条
     * @param duration
     */
    private void onDurationChanged(int duration) {
        mDuration = duration;
        String totalTime = Utils.stringForTime(duration);
        mTotalTimeTextView.setText(totalTime);
        mHandler.sendEmptyMessage(ProgressHandler.UPDATE_BOTTOM_PROGRESS);
    }

    private void onPlayStateChanged(int state) {
        mCurrentState = state;
        onChangeUIState(state);
    }

    public void onChangeUIState(int state) {
        switch (state) {
            case PlayState.STATE_NORMAL:
                changeUINormal();
                break;
            case PlayState.STATE_LOADING:
                changeUILoading();
                break;
            case PlayState.STATE_PLAYING:
                changeUIPlay();
                break;
            case PlayState.STATE_PAUSE:
                changeUIPause();
                break;
            case PlayState.STATE_PLAYING_BUFFERING_START:
                changeUIBuffer();
                break;
            case PlayState.STATE_AUTO_COMPLETE:
                changeUICompeted();
                break;
            case PlayState.STATE_ERROR:
                changeUIError();
                break;
            case PlayState.STATE_NET_ERROR:
                changeUINetError();
                break;
            default:
                throw new IllegalStateException("Illegal Play State:" + state);
        }
    }

    @Subscribe
    public void onKeyEvent(KeyMsgEvent msg) {
        if (msg != null && msg.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Activity activity = (Activity) getContext();
            mPresenter.handleScreenRotate(activity.getRequestedOrientation());
            return;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Activity activity = (Activity) getContext();
            mPresenter.handleScreenRotate(activity.getRequestedOrientation());
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public ImageView getThumbImageView() {

        return mVideoThumbView;
    }

    public TextureView createTextureView() {
        //重新为播放器关联TextureView
        TextureView textureView = newTextureView();
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureView.setLayoutParams(params);
        return textureView;
    }

    /**
     * 创建一个TextureView
     * 此处单独写成一个方法可以方便后续自定义扩展TextureView
     *
     * @return
     */
    protected TextureView newTextureView() {

        return new TextureView(getContext());
    }


    /************************ UI状态更新 ********************************/

    @Override
    public void changeUICompeted() {
    //显示视频预览图
        Utils.showViewIfNeed(mVideoThumbView);
        Utils.showViewIfNeed(mStartButton);
        mStartButton.setImageResource(R.drawable.news_video_start);
    }

    @Override
    public void changeUIError() {

    }

    @Override
    public void changeUILoading() {

        Utils.hideViewIfNeed(mStartButton);
        Utils.showViewIfNeed(mLoadingView);
    }

    @Override
    public void changeUINetError() {

    }

    @Override
    public void changeUINormal() {

        //显示视频预览图
        Utils.showViewIfNeed(mVideoThumbView);

        //显示视频标题
        Utils.showViewIfNeed(mTitleTextView);
        //隐藏视频返回键
        Utils.hideViewIfNeed(mBackButton);

        Utils.hideViewIfNeed(mTopStatusView);
        //底部布局隐藏
        mBottomContainer.setVisibility(View.INVISIBLE);
        //loading 布局隐藏
        mLoadingView.setVisibility(View.INVISIBLE);
        //锁屏按钮隐藏
        mLockBtn.setVisibility(GONE);
        //重播按钮隐藏
        mReplayView.setVisibility(GONE);
        //progressbar 隐藏
        mBottomProgressBar.setVisibility(View.INVISIBLE);
        //开始按钮显示
        mStartButton.setVisibility(View.VISIBLE);
        mStartButton.setImageResource(R.drawable.news_video_start);
    }

    @Override
    public void changeUIPause() {
        //顶部返回键显示
        mBackButton.setVisibility(View.VISIBLE);
        //顶部title显示
        Utils.showViewIfNeed(mTitleTextView);
        //loading 布局隐藏
        mLoadingView.setVisibility(View.INVISIBLE);
        //开始按钮显示
        mStartButton.setVisibility(View.VISIBLE);
        mStartButton.setImageResource(R.drawable.news_video_start);
        //底部控制布局显示
        mBottomContainer.setVisibility(View.VISIBLE);
        //底部progressbar 隐藏
        mBottomProgressBar.setVisibility(View.INVISIBLE);
        mHandler.removeMessages(ProgressHandler.UPDATE_CONTROLLER_VIEW);
    }

    @Override
    public void changeUIPlay() {
        Utils.showViewIfNeed(mStartButton);
        mStartButton.setImageResource(R.drawable.news_video_pause);
        //隐藏视频预览图
        Utils.hideViewIfNeed(mVideoThumbView);

        Utils.hideViewIfNeed(mReplayView);
        Utils.hideViewIfNeed(mLoadingView);

        mHandler.sendEmptyMessageDelayed(ProgressHandler.UPDATE_CONTROLLER_VIEW, ProgressHandler.AUDO_HIDE_WIDGET_TIME);
        mHandler.sendEmptyMessage(ProgressHandler.UPDATE_BOTTOM_PROGRESS);
    }

    @Override
    public void changeUIBuffer() {
        Utils.showViewIfNeed(mLoadingView);
        Utils.hideViewIfNeed(mStartButton);
    }

    @Override
    public void hidenAllView() {
        Utils.hideViewIfNeed(mTitleTextView);
        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mBottomContainer);
    }

    @Override
    public void showAllView() {
        Utils.showViewIfNeed(mTitleTextView);
        Utils.showViewIfNeed(mStartButton);
        Utils.showViewIfNeed(mBottomContainer);
        mHandler.removeMessages(ProgressHandler.UPDATE_CONTROLLER_VIEW);
        android.os.Message msg = new android.os.Message();
        msg.what = ProgressHandler.UPDATE_CONTROLLER_VIEW;
        msg.arg1 = ScreenState.SCREEN_STATE_NORMAL;
        mHandler.sendMessageDelayed(msg, ProgressHandler.AUDO_HIDE_WIDGET_TIME);
    }

    @Override
    public void hideViewInFullScreenState() {
        Utils.hideViewIfNeed(mTitleTextView);
        Utils.hideViewIfNeed(mBackButton);
        Utils.hideViewIfNeed(mWifiView);
        Utils.hideViewIfNeed(mBatteryView);
        Utils.hideViewIfNeed(mTimeView);
        Utils.hideViewIfNeed(mReplayView);
        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mBottomContainer);
        Utils.hideViewIfNeed(mLockBtn);
    }

    @Override
    public void showViewInFullScreenState() {
        Utils.showViewIfNeed(mBackButton);
        Utils.showViewIfNeed(mTitleTextView);
        Utils.showViewIfNeed(mWifiView);
        Utils.showViewIfNeed(mBatteryView);
        Utils.showViewIfNeed(mTimeView);
        Utils.showViewIfNeed(mStartButton);
        Utils.showViewIfNeed(mBottomContainer);
        Utils.showViewIfNeed(mLockBtn);
        mHandler.removeMessages(ProgressHandler.UPDATE_CONTROLLER_VIEW);
        android.os.Message msg = new android.os.Message();
        msg.what = ProgressHandler.UPDATE_CONTROLLER_VIEW;
        msg.arg1 = ScreenState.SCREEN_STATE_FULLSCREEN;
        mHandler.sendMessageDelayed(msg, ProgressHandler.AUDO_HIDE_WIDGET_TIME);
    }

    @Override
    public void changeUIFullScreen() {
        mToggleFullScreen = true;
        PlayerManager.getInstance().setScreenState(mCurrentScreenState = ScreenState.SCREEN_STATE_FULLSCREEN);
        PlayerManager.getInstance().pause();

        ViewGroup windowContent = (ViewGroup) (Utils.getActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        mVideoWidth = this.getWidth();
        mVideoHeight = this.getHeight();
        mOldParent = (ViewGroup)this.getParent();
        mOldIndex = mOldParent.indexOfChild(this);
        mOldParent.removeView(this);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        windowContent.addView(this, lp);

        Utils.getActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        Utils.getActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        PlayerManager.getInstance().play();

        mFullscreenButton.setImageResource(R.drawable.news_video_full_off);

        Utils.showViewIfNeed(mLockBtn);
        Utils.showViewIfNeed(mWifiView);
        Utils.showViewIfNeed(mBatteryView);
        Utils.showViewIfNeed(mTimeView);
        Utils.showViewIfNeed(mTopStatusView);
    }

    @Override
    public void changeUINormalScreen() {
        if(!ScreenState.isFullScreen(mCurrentScreenState)) {
            return;
        }
        mToggleFullScreen = true;
        PlayerManager.getInstance().setScreenState(mCurrentScreenState = ScreenState.SCREEN_STATE_NORMAL);
        PlayerManager.getInstance().pause();

        ViewGroup windowContent = (ViewGroup) (Utils.getActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        windowContent.removeView(this);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mVideoWidth, mVideoHeight);
        mOldParent.addView(this, mOldIndex, lp);

        Utils.getActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        Utils.getActivity(getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mFullscreenButton.setImageResource(R.drawable.news_video_full_on);
        mOldParent = null;
        mOldIndex = 0;
        if(mCurrentState != PlayState.STATE_AUTO_COMPLETE) {
            PlayerManager.getInstance().play();
        }
        Utils.hideViewIfNeed(mLockBtn);
        Utils.hideViewIfNeed(mBackButton);
        Utils.hideViewIfNeed(mWifiView);
        Utils.hideViewIfNeed(mBatteryView);
        Utils.hideViewIfNeed(mTimeView);
        Utils.hideViewIfNeed(mTopStatusView);
    }

    @Override
    public void changeUIErrorToast() {
        Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void changeUILock() {
        mLockBtn.setImageResource(R.drawable.news_video_lock_on);
        Utils.hideViewIfNeed(mTitleTextView);
        Utils.hideViewIfNeed(mBackButton);
        Utils.hideViewIfNeed(mWifiView);
        Utils.hideViewIfNeed(mBatteryView);
        Utils.hideViewIfNeed(mTimeView);
        Utils.hideViewIfNeed(mReplayView);
        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mBottomContainer);
        Utils.showViewIfNeed(mLockBtn);
    }

    @Override
    public void changeuiUnLock() {
        mLockBtn.setImageResource(R.drawable.news_video_lock_off);
        Utils.showViewIfNeed(mTitleTextView);
        Utils.showViewIfNeed(mStartButton);
        Utils.showViewIfNeed(mBottomContainer);
        Utils.showViewIfNeed(mBackButton);
    }

    @Override
    public void showPositionLiftAnimation(String seekTime, String totalTime) {
        Utils.showViewIfNeed(mPopView);
        Utils.showViewIfNeed(mPopIconView);
        Utils.showViewIfNeed(mPopTimeCtrlView);
        Utils.showViewIfNeed(mPopPregressBar);
        Utils.showViewIfNeed(mPopCurTimeView);
        Utils.showViewIfNeed(mPopTotalTimeView);

        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mLoadingView);
        Utils.hideViewIfNeed(mPopPregressBar);

        mPopCurTimeView.setText(seekTime);
        mPopTotalTimeView.setText(" / " + totalTime);
        mPopIconView.setImageResource(R.drawable.news_video_gesture_backward);
    }

    @Override
    public void showPositionRightAnimation(String seekTime, String totalTime) {
        Utils.showViewIfNeed(mPopView);
        Utils.showViewIfNeed(mPopIconView);
        Utils.showViewIfNeed(mPopTimeCtrlView);
        Utils.showViewIfNeed(mPopCurTimeView);
        Utils.showViewIfNeed(mPopTotalTimeView);

        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mPopPregressBar);
        Utils.hideViewIfNeed(mLoadingView);

        mPopCurTimeView.setText(seekTime);
        mPopTotalTimeView.setText(" / " + totalTime);
        mPopIconView.setImageResource(R.drawable.news_video_gesture_forward);
    }

    @Override
    public void changeVolumeAnimation() {
        Utils.showViewIfNeed(mPopView);
        Utils.showViewIfNeed(mPopIconView);
        Utils.showViewIfNeed(mPopContentView);
        Utils.showViewIfNeed(mPopPregressBar);

        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mPopTimeCtrlView);
        Utils.hideViewIfNeed(mLoadingView);
        mPopIconView.setImageResource(R.drawable.news_video_gesture_volume);
    }


    @Override
    public void changeMediaVolume(float deltaY) {
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager
                .STREAM_MUSIC);
        int mAudioMaxValue = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int deltaV = (int) (mAudioMaxValue * deltaY * 3 / mScreenHight);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume +
                deltaV, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        int volumePercent = (int) (mGestureDownVolume * 100 / mAudioMaxValue + deltaY * 3 *
                100 / mScreenHight);

        mPopPregressBar.setProgress(volumePercent);
    }

    @Override
    public void changeScreenBrightness(float deltaY) {
        Activity activity = (Activity) getContext();
        float brightnessData = activity.getWindow().getAttributes().screenBrightness;
        if (brightnessData <= 0.00f) {
            brightnessData = 0.50f;
        } else if (brightnessData < 0.01f) {
            brightnessData = 0.01f;
        }
        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = brightnessData + deltaY;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        activity.getWindow().setAttributes(lpa);
    }

    @Override
    public void showBrightnessAnimation() {
        Utils.showViewIfNeed(mPopView);
        Utils.showViewIfNeed(mPopContentView);
        Utils.showViewIfNeed(mPopIconView);
        Utils.showViewIfNeed(mPopPregressBar);

        Utils.hideViewIfNeed(mStartButton);
        Utils.hideViewIfNeed(mLoadingView);
        Utils.hideViewIfNeed(mPopTimeCtrlView);
        mPopIconView.setImageResource(R.drawable.news_video_gesture_brightness);
        Activity activity = (Activity) getContext();
        float percent = activity.getWindow().getAttributes().screenBrightness * 100;
        mPopPregressBar.setProgress((int) percent);
    }

    @Override
    public void hidePopView() {
        Utils.hideViewIfNeed(mPopView);
        Utils.hideViewIfNeed(mPopTimeCtrlView);
        Utils.hideViewIfNeed(mPopPregressBar);
        Utils.hideViewIfNeed(mPopIconView);
        Utils.hideViewIfNeed(mPopContentView);
        Utils.hideViewIfNeed(mPopCurTimeView);
        Utils.hideViewIfNeed(mPopTotalTimeView);
    }

    /************************ 定时处理的工具 ********************************/

    /**
     * reset buffer view text to zero.
     */
    public void resetBufferProcess() {
        if (mBufferTextView != null) {
            mBufferTextView.setText(getResources().getString(R.string.text_buffer));
        }
    }


    /**
     * 更新loading速度
     */
    public void updateLoadingProgress(android.os.Message msg) {

        //TODO 根据IjkPlayer 获取
        int bufferCount = msg.arg1;
        bufferCount = (bufferCount > 100) ? 100 : bufferCount;
        String bufferText = bufferCount + "%";
        mBufferTextView.setText(bufferText);
        mBufferTextView.setVisibility(VISIBLE);
        mHandler.sendEmptyMessageDelayed(ProgressHandler.UPDATE_LOADING_SPEED, 500);
    }


    /**
     * 更新底部控制布局中的seekbar
     */
    public void updateBottomProgress() {
        int position = PlayerManager.getInstance().getCurrentPosition();
        int totalTime = mDuration;
        int progress = position * 100 / (totalTime == 0 ? 1 : totalTime);

        if (progress != 0) {
            //可见的seekbar
            mBottomSeekBar.setProgress(progress);
            //底部最低的progressbar
            mBottomProgressBar.setProgress(progress);
        }

        if (mTotalTimeTextView != null && TextUtils.isEmpty(mTotalTimeTextView.getText().toString())) {
            mTotalTimeTextView.setText(Utils.stringForTime(totalTime));
        }
        if (position > 0) {
            mBottomCurTimeView.setText(Utils.stringForTime(position));
        }

        if (!mHandler.hasMessages (ProgressHandler.UPDATE_BOTTOM_PROGRESS) && (PlayerManager.getInstance().isPlaying() || mCurrentState == PlayState.STATE_PLAYING)) {
            mHandler.sendEmptyMessageDelayed(ProgressHandler.UPDATE_BOTTOM_PROGRESS, 500);
        }
    }


    /**
     * 更新视频控制界面
     */
    public void updateControllerView(android.os.Message msg) {
        if (mCurrentState != PlayState.STATE_NORMAL
                && mCurrentState != PlayState.STATE_ERROR
                && mCurrentState != PlayState.STATE_AUTO_COMPLETE) {
            if (msg.arg1 ==ScreenState.SCREEN_STATE_FULLSCREEN) {
                hideViewInFullScreenState();
            } else if (msg.arg1 == ScreenState.SCREEN_STATE_NORMAL) {
                hidenAllView();
            }
        }

    }


    @Override
    public void cancleDismissControlViewTimer() {
        if (mHandler != null) {
            mHandler.removeMessages(ProgressHandler.UPDATE_CONTROLLER_VIEW);
        }
    }

    @Override
    public void startDismissControlViewTimer() {
        if (mHandler != null) {
            android.os.Message msg = new android.os.Message();
            msg.what = ProgressHandler.UPDATE_CONTROLLER_VIEW;
            msg.arg1 = ScreenState.SCREEN_STATE_FULLSCREEN;
            mHandler.sendMessageDelayed(msg, ProgressHandler.AUDO_HIDE_WIDGET_TIME);
        }
    }

    /**
     * 开始播放视频
     */
    public void startPlayVideo() {

        if(!Utils.isConnected(getContext())) {
            if(!PlayerManager.getInstance().isCached(mVideoUrl)) {
                Toast.makeText(getContext(), R.string.vp_no_network, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        ((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestAudioFocus();
        //先移除播放器关联的TextureView
        PlayerManager.getInstance().removeTextureView();

        TextureView textureView = createTextureView();
        mTextureViewContainer.addView(textureView);
        //准备开始播放
        PlayerManager.getInstance().start(mVideoUrl, mViewHash);
        PlayerManager.getInstance().setTextureView(textureView);
    }

    /**
     * 请求获取AudioFocus
     */
    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }
}
