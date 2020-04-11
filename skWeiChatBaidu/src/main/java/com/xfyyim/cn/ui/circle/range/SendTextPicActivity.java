package com.xfyyim.cn.ui.circle.range;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSON;
import com.xfyyim.cn.AppConstant;
import com.xfyyim.cn.MyApplication;
import com.xfyyim.cn.R;
import com.xfyyim.cn.adapter.SendTopicAdapter;
import com.xfyyim.cn.bean.Area;
import com.xfyyim.cn.bean.UploadFileResult;
import com.xfyyim.cn.helper.DialogHelper;
import com.xfyyim.cn.helper.ImageLoadHelper;
import com.xfyyim.cn.helper.LoginHelper;
import com.xfyyim.cn.helper.UploadService;
import com.xfyyim.cn.sp.UserSp;
import com.xfyyim.cn.ui.account.LoginActivity;
import com.xfyyim.cn.ui.base.BaseActivity;
import com.xfyyim.cn.ui.circle.util.SendTextFilter;
import com.xfyyim.cn.ui.tool.MultiImagePreviewActivity;
import com.xfyyim.cn.util.DeviceInfoUtil;
import com.xfyyim.cn.util.ToastUtil;
import com.xfyyim.cn.video.EasyCameraActivity;
import com.xfyyim.cn.video.MessageEventGpu;
import com.xfyyim.cn.view.SquareCenterFrameLayout;
import com.xfyyim.cn.view.photopicker.PhotoPickerActivity;
import com.xfyyim.cn.view.photopicker.SelectModel;
import com.xfyyim.cn.view.photopicker.intent.PhotoPickerIntent;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class SendTextPicActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;  // 拍照
    private static final int REQUEST_CODE_PICK_PHOTO = 2;     // 图库
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    Unbinder unbinder;
    @BindView(R.id.tv_title_right)
    TextView tv_title_right;
    @BindView(R.id.tv_title_center)
    TextView tv_title_center;
    @BindView(R.id.tv_Location)
    TextView tv_Location;
    @BindView(R.id.iv_title_left)
    ImageView iv_title_left;
    @BindView(R.id.rcv_img)
    RecyclerView rcvImg;

    @BindView(R.id.img_blum)
    ImageView img_blum;

    @BindView(R.id.img_camera)
    ImageView img_camera;

    @BindView(R.id.img_locotion)
    ImageView img_locotion;
    @BindView(R.id.tv)
       TextView tv;

    @BindView(R.id.rv_add_topic)
    RecyclerView rv_add_topic;


    @BindView(R.id.et_content)
            EditText editText;



    PostArticleImgAdapter postArticleImgAdapter;
    private ArrayList<String> mPhotoList;
    // 拍照和图库，获得图片的Uri
    private Uri mNewPhotoUri;

    private String address;

    private ItemTouchHelper itemTouchHelper;

    private String mImageData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_show);
        unbinder = ButterKnife.bind(this);

        initActionBar();
        mPhotoList = new ArrayList<>();
        postArticleImgAdapter = new PostArticleImgAdapter(this, mPhotoList);
        initRcv();
        EventBus.getDefault().register(this);



        // 解决EditText与ScrollView嵌套的问题
        editText.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        // 限制了EditText输入最大长度为300，到达限制时提示
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editText.getText().toString().trim().length() >= 300) {
                    Toast.makeText(mContext, getString(R.string.tip_edit_max_input_length, 10000), Toast.LENGTH_SHORT).show();
                }
            }
        });


    }


    private void initActionBar() {
        getSupportActionBar().hide();
        iv_title_left.setVisibility(View.VISIBLE);
        iv_title_left.setOnClickListener(this);

        tv_title_center.setText("写动态");
        tv_title_right.setText("发布");
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(this);
        img_blum.setOnClickListener(this);
        img_camera.setOnClickListener(this);
        img_locotion.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.iv_title_left:
                finish();
                break;
            case R.id.tv_title_right:

                if (mPhotoList.size() <= 0 && TextUtils.isEmpty(editText.getText().toString())) {
                    return;
                }
                if (mPhotoList.size() <= 0) {
                    // 发布文字
                    sendShuoshuo();
                } else {
                    // 发布图片+文字
                    new  UploadPhoto().execute();
                }
                break;

            case R.id.img_blum:
                selectPhoto();
                break;
            case R.id.img_camera:
                takePhoto();
                break;
            case R.id.img_locotion:
                break;
        }
    }


    // 发布一条说说
    public void sendShuoshuo() {
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();

        if (TextUtils.isEmpty(editText.getText().toString().trim()) && mPhotoList.size() == 0) {
            DialogHelper.tip(mContext, getString(R.string.leave_select_image_or_edit_text));
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", UserSp.getInstance(SendTextPicActivity.this).getAccessToken());
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        if (TextUtils.isEmpty(mImageData)) {
            params.put("type", "1");
        } else {
            params.put("type", "2");
        }
        // 消息标记：1：求职消息；2：招聘消息；3：普通消息；
        params.put("flag", "3");

//        // 消息隐私范围：1=公开；2=私密；3=部分选中好友可见；4=不给谁看
//        params.put("visible", String.valueOf(visible));
//        if (visible == 3) {
//            // 谁可以看
//            params.put("userLook", lookPeople);
//        } else if (visible == 4) {
//            // 不给谁看
//            params.put("userNotLook", lookPeople);
//        }
//        // 提醒谁看
//        if (!TextUtils.isEmpty(atlookPeople)) {
//            params.put("userRemindLook", atlookPeople);
//        }

        // 消息内容
        params.put("text", SendTextFilter.filter(editText.getText().toString()));
        if (!TextUtils.isEmpty(mImageData)) {
            // 图片
            params.put("images", mImageData);
        }

        params.put("isAllowComment",  String.valueOf(1) );
        /**
         * 所在位置
         */
        if (!TextUtils.isEmpty(address)) {
            // 纬度
            params.put("latitude", String.valueOf(latitude));
            // 经度
            params.put("longitude", String.valueOf(longitude));
            // 位置
            params.put("location", address);
        }
        // 纬度
        params.put("latitude", String.valueOf(latitude));
        // 经度
        params.put("longitude", String.valueOf(longitude));
        // 位置


        // 必传，之前删除该字段，发布说说，服务器返回接口内部异常
        Area area = Area.getDefaultCity();
        if (area != null) {
            // 城市id
            params.put("cityId", String.valueOf(area.getId()));
        } else {
            params.put("cityId", "0");
        }

        /**
         * 附加信息
         */
        // 手机型号
        params.put("model", DeviceInfoUtil.getModel());
        // 手机操作系统版本号
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        if (!TextUtils.isEmpty(DeviceInfoUtil.getDeviceId(mContext))) {
            // 设备序列号
            params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));
        }

        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.post().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(mContext, result)) {
                            Intent intent = new Intent();
                            intent.putExtra(AppConstant.EXTRA_MSG_ID, result.getData());
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(SendTextPicActivity.this);
                    }
                });
    }

    //发表图文
    private class UploadPhoto extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(SendTextPicActivity.this);
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 上传出错<br/>
         * return 3 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            Map<String, String> mapParams = new HashMap<>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// 文件有效期

            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, mapParams, mPhotoList);
            if (TextUtils.isEmpty(result)) {
                return 2;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(SendTextPicActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {
                    // 上传丢失了某些文件
                    return 2;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    } else {
                        return 2;
                    }
                    return 3;
                } else {
                    // 没有文件数据源，失败
                    return 2;
                }
            } else {
                return 2;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                DialogHelper.dismissProgressDialog();
                startActivity(new Intent(SendTextPicActivity.this, LoginActivity.class));
            } else if (result == 2) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendTextPicActivity.this, getString(R.string.upload_failed));
            } else {
                sendShuoshuo();
            }
        }
    }



    private void initRcv() {
        rcvImg.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        rcvImg.setAdapter(postArticleImgAdapter);
       MyCallBack myCallBack = new MyCallBack(tv, postArticleImgAdapter, mPhotoList, null);
        itemTouchHelper = new ItemTouchHelper(myCallBack);
        itemTouchHelper.attachToRecyclerView(rcvImg);//绑定RecyclerView
        //事件监听
        rcvImg.addOnItemTouchListener(new  OnRecyclerItemClickListener(rcvImg) {

            @Override
            public void onItemClick(RecyclerView.ViewHolder vh) {
                showPictureActionDialog(vh.getAdapterPosition());

            }

            @Override
            public void onItemLongClick(RecyclerView.ViewHolder vh) {
                //如果item不是最后一个，则执行拖拽
                if (vh.getLayoutPosition() != mPhotoList.size()) {
                    itemTouchHelper.startDrag(vh);
                }
            }
        });

        myCallBack.setDragListener(new  DragListener() {
            @Override
            public void deleteState(boolean delete) {
                if (delete) {
                    tv.setBackgroundResource(R.color.holo_red_dark);
                    tv.setText(getResources().getString(R.string.post_delete_tv_s));
                } else {
                    tv.setText(getResources().getString(R.string.post_delete_tv_d));
                    tv.setBackgroundResource(R.color.holo_red_light);
                }
            }

            @Override
            public void dragState(boolean start) {
                if (start) {
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.GONE);
                }
            }

            @Override
            public void clearView() {

            }
        });
    }
    public static class MyCallBack extends ItemTouchHelper.Callback {

        private int dragFlags;
        private int swipeFlags;
        private View removeView;
        private PostArticleImgAdapter adapter;
        private List<String> images;//图片经过压缩处理
        private List<String> originImages;//图片没有经过处理，这里传这个进来是为了使原图片的顺序与拖拽顺序保持一致
        private boolean up;//手指抬起标记位
        private DragListener dragListener;

        public MyCallBack(View tv, PostArticleImgAdapter adapter, List<String> images, List<String> originImages) {
            removeView = tv;
            this.adapter = adapter;
            this.images = images;
            this.originImages = originImages;
        }

        /**
         * 设置item是否处理拖拽事件和滑动事件，以及拖拽和滑动操作的方向
         *
         * @param recyclerView
         * @param viewHolder
         * @return
         */
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //判断 recyclerView的布局管理器数据
            if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {//设置能拖拽的方向
                dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                swipeFlags = 0;//0则不响应事件
            }
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        /**
         * 当用户从item原来的位置拖动可以拖动的item到新位置的过程中调用
         *
         * @param recyclerView
         * @param viewHolder
         * @param target
         * @return
         */
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();//得到item原来的position
            int toPosition = target.getAdapterPosition();//得到目标position
            if (toPosition == images.size() || images.size() == fromPosition) {
                return true;
            }
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(images, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(images, i, i - 1);
                }
            }
            adapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        /**
         * 设置是否支持长按拖拽
         *
         * @return
         */
        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        /**
         * @param viewHolder
         * @param direction
         */
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        /**
         * 当用户与item的交互结束并且item也完成了动画时调用
         *
         * @param recyclerView
         * @param viewHolder
         */
        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            adapter.notifyDataSetChanged();
            initData();
            if (dragListener != null) {
                dragListener.clearView();
            }
        }

        /**
         * 重置
         */
        private void initData() {
            if (dragListener != null) {
                dragListener.deleteState(false);
                dragListener.dragState(false);
            }
            up = false;
        }

        /**
         * 自定义拖动与滑动交互
         *
         * @param c
         * @param recyclerView
         * @param viewHolder
         * @param dX
         * @param dY
         * @param actionState
         * @param isCurrentlyActive
         */
        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (null == dragListener) {
                return;
            }
            int[] location = new int[2];
            viewHolder.itemView.getLocationInWindow(location); //获取在当前窗口内的绝对坐标
            if (location[1] + viewHolder.itemView.getHeight() > removeView.getTop()) {//拖到删除处
                dragListener.deleteState(true);
                if (up) {//在删除处放手，则删除item
                    viewHolder.itemView.setVisibility(View.INVISIBLE);//先设置不可见，如果不设置的话，会看到viewHolder返回到原位置时才消失，因为remove会在viewHolder动画执行完成后才将viewHolder删除
                    images.remove(viewHolder.getAdapterPosition());
                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    initData();
                    return;
                }
            } else {//没有到删除处
                if (View.INVISIBLE == viewHolder.itemView.getVisibility()) {//如果viewHolder不可见，则表示用户放手，重置删除区域状态
                    dragListener.dragState(false);
                }
                dragListener.deleteState(false);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        /**
         * 当长按选中item的时候（拖拽开始的时候）调用
         *
         * @param viewHolder
         * @param actionState
         */
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (ItemTouchHelper.ACTION_STATE_DRAG == actionState && dragListener != null) {
                dragListener.dragState(true);
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        /**
         * 设置手指离开后ViewHolder的动画时间，在用户手指离开后调用
         *
         * @param recyclerView
         * @param animationType
         * @param animateDx
         * @param animateDy
         * @return
         */
        @Override
        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            //手指放开
            up = true;
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        void setDragListener(DragListener dragListener) {
            this.dragListener = dragListener;
        }

    }



    /**
     * 相册
     * 可以多选的图片选择器
     */
    private void selectPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(SendTextPicActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // 是否显示拍照， 默认false
        intent.setShowCarema(false);
        // 最多选择照片数量，默认为9
        intent.setMaxTotal(9 - mPhotoList.size());
        // 已选中的照片地址， 用于回显选中状态
        intent.setSelectedPaths(imagePaths);
        // intent.setImageConfig(config);
        // 是否加载视频，默认true
        intent.setLoadVideo(false);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }




    private void showPictureActionDialog(final int position) {
        String[] items = new String[]{getString(R.string.look_over), getString(R.string.delete)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(getString(R.string.image))
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // 查看
                            Intent intent = new Intent(SendTextPicActivity.this, MultiImagePreviewActivity.class);
                            intent.putExtra(AppConstant.EXTRA_IMAGES, mPhotoList);
                            intent.putExtra(AppConstant.EXTRA_POSITION, position);
                            intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
                            startActivity(intent);
                        } else {
                            // 删除
                            deletePhoto(position);
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void deletePhoto(final int position) {
        mPhotoList.remove(position);
        postArticleImgAdapter.notifyDataSetChanged();
    }

    // 拍照
    private void takePhoto() {
        Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
            // 拍照返回 Todo 已更换拍照方式
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    photograph(new File(mNewPhotoUri.getPath()));
                } else {
                    ToastUtil.showToast(this, R.string.c_take_picture_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            // 选择图片返回
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                    album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        }

//        else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
//            // 选择位置返回
//            latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
//            longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
//            address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
//            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
//                Log.e("zq", "纬度:" + latitude + "   经度：" + longitude + "   位置：" + address);
//                tv_Location.setText(address);
//            } else {
//                ToastUtil.showToast(mContext, getString(R.string.loc_startlocnotice));
//            }
//        }



    }


    // 单张图片压缩 拍照
    private void photograph(final File file) {
        Log.e("zq", "压缩前图片路径:" + file.getPath() + "压缩前图片大小:" + file.length() / 1024 + "KB");
        // 拍照出来的图片Luban一定支持，
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // 原图小于100kb 不压缩
                // .putGear(2)     // 设定压缩档次，默认三挡
                // .setTargetDir() // 指定压缩后的图片路径
                .setCompressListener(new OnCompressListener() { //设置回调
                    @Override
                    public void onStart() {
                        Log.e("zq", "开始压缩");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "压缩成功，压缩后图片位置:" + file.getPath() + "压缩后图片大小:" + file.length() / 1024 + "KB");
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "压缩失败,原图上传");
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();
                    }
                }).launch();// 启动压缩
    }

    // 多张图片压缩 相册
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// 原图发送，不压缩
            Log.e("zq", "原图上传，不压缩，选择原文件路径");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                mPhotoList.add(stringArrayListExtra.get(i));
                postArticleImgAdapter.notifyDataSetChanged();
            }
            return;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // Luban只处理特定后缀的图片，不满足的不处理也不走回调，
            // 只能挑出来不压缩，
            // todo luban支持压缩.gif图，但是压缩之后的.gif图用glide加载与转换为gifDrawable都会出问题，所以,gif图不压缩了
            List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp");
            boolean support = false;
            for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                    support = true;
                    break;
                }
            }
            if (!support) {
                list.add(stringArrayListExtra.get(i));
            }
        }

        if (list.size() > 0) {
            for (String s : list) {// 不压缩的部分，直接发送
                mPhotoList.add(s);
                postArticleImgAdapter.notifyDataSetChanged();

            }
        }

        // 移除掉不压缩的图片
        stringArrayListExtra.removeAll(mPhotoList);

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// 原图小于100kb 不压缩
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// 启动压缩
    }





    class PostArticleImgAdapter extends RecyclerView.Adapter<PostArticleImgAdapter.MyViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<String> mDatas;

        public PostArticleImgAdapter(Context context, List<String> datas) {
            this.mDatas = datas;
            this.mContext = context;
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public PostArticleImgAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PostArticleImgAdapter.MyViewHolder(mLayoutInflater.inflate(R.layout.item_post_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(final PostArticleImgAdapter.MyViewHolder holder, final int position) {
                holder.squareCenterFrameLayout.setVisibility(View.GONE);
                ImageLoadHelper.showImageWithSizeError(
                        mContext,
                        mDatas.get(position),
                        R.drawable.pic_error,
                        150, 150,
                        holder.imageView
                );
        }

        @Override
        public int getItemCount() {
            if (mDatas.size() >= 9) {
                return 9;
            }
            return mDatas.size() ;
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            SquareCenterFrameLayout squareCenterFrameLayout;

            MyViewHolder(View itemView) {
                super(itemView);
                squareCenterFrameLayout = itemView.findViewById(R.id.add_sc);
                imageView = itemView.findViewById(R.id.sdv);
            }
        }
    }



    public abstract class OnRecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        private GestureDetectorCompat mGestureDetector;
        private RecyclerView recyclerView;

        public OnRecyclerItemClickListener(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
            mGestureDetector = new GestureDetectorCompat(recyclerView.getContext(), new OnRecyclerItemClickListener.ItemTouchHelperGestureListener());
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            if (mGestureDetector.onTouchEvent(e)) {
                return true;
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        public abstract void onItemClick(RecyclerView.ViewHolder vh);

        public abstract void onItemLongClick(RecyclerView.ViewHolder vh);

        private class ItemTouchHelperGestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                    onItemClick(vh);
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                    onItemLongClick(vh);
                }
            }
        }
    }


    interface DragListener {
        /**
         * 用户是否将 item拖动到删除处，根据状态改变颜色
         *
         * @param delete
         */
        void deleteState(boolean delete);

        /**
         * 是否于拖拽状态
         *
         * @param start
         */
        void dragState(boolean start);

        /**
         * 当用户与item的交互结束并且item也完成了动画时调用
         */
        void clearView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (unbinder!=null)
          unbinder.unbind();
    }

   public void  setTopicAdapter(List<String > list){
       LinearLayoutManager linearLayoutManager=new LinearLayoutManager(SendTextPicActivity.this);
       linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
       rv_add_topic.setLayoutManager(linearLayoutManager);

       SendTopicAdapter adapter=new SendTopicAdapter(list);

    }




}
