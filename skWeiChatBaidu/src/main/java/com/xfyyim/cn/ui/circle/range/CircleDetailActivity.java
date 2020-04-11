package com.xfyyim.cn.ui.circle.range;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.stx.xhb.xbanner.XBanner;
import com.xfyyim.cn.MyApplication;
import com.xfyyim.cn.R;
import com.xfyyim.cn.Reporter;
import com.xfyyim.cn.adapter.UserClickableSpan;
import com.xfyyim.cn.bean.Friend;
import com.xfyyim.cn.bean.circle.Comment;
import com.xfyyim.cn.bean.circle.Praise;
import com.xfyyim.cn.bean.circle.PublicMessage;
import com.xfyyim.cn.db.dao.FriendDao;
import com.xfyyim.cn.helper.AvatarHelper;
import com.xfyyim.cn.ui.base.BaseActivity;
import com.xfyyim.cn.ui.base.CoreManager;
import com.xfyyim.cn.ui.circle.BusinessCircleActivity;
import com.xfyyim.cn.ui.circle.MessageEventReply;
import com.xfyyim.cn.util.HtmlUtils;
import com.xfyyim.cn.util.LinkMovementClickMethod;
import com.xfyyim.cn.util.StringUtils;
import com.xfyyim.cn.util.SystemUtil;
import com.xfyyim.cn.util.TimeUtils;
import com.xfyyim.cn.util.ToastUtil;
import com.xfyyim.cn.util.glideUtil.GlideImageUtils;
import com.xfyyim.cn.view.CheckableImageView;
import com.xfyyim.cn.view.TrillCommentInputDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class CircleDetailActivity extends BaseActivity {
    @BindView(R.id.iv_title_left)
    ImageView iv_title_left;
    @BindView(R.id.tv_title_center)
    TextView tv_title_center;

    @BindView(R.id.command_listView)
    ListView command_listView;
    @BindView(R.id.banner)
    XBanner banner;
    @BindView(R.id.avatar_img)
    ImageView avatar_img;
    @BindView(R.id.img_vip)
    ImageView img_vip;
    @BindView(R.id.nick_name_tv)
    TextView nick_name_tv;
    @BindView(R.id.tv_content)
    TextView tv_content;
    @BindView(R.id.tv_age)
    TextView tv_age;
    @BindView(R.id.time_distance)
    TextView time_distance;
    @BindView(R.id.tvLoadMore)
    TextView tvLoadMore;
    @BindView(R.id.tvThumb)
    TextView tvThumb; @BindView(R.id.tv_comment)
    TextView tv_comment;
    @BindView(R.id.tv_read_count)
    TextView tv_read_count;
    @BindView(R.id.img_sex)
    ImageView img_sex;
    @BindView(R.id.llThumb)
    LinearLayout llThumb;
    @BindView(R.id.ivThumb)


    CheckableImageView ivThumb;
    Unbinder unbinder;
    CommentAdapter adapter;
    List<Comment> data;
    private String mLoginUserId;
    PublicMessage message;
    private String mUserId;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circledetail);

        unbinder = ButterKnife.bind(this);
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();
        initView();
        onBindLinstener();
        loadCommentsNextPage(message.getMessageId());

    }


    public void initView() {
        List<String> imgesUrl = new ArrayList<>();
        message = (PublicMessage) getIntent().getSerializableExtra("PublicMessage");
        mLoginUserId = coreManager.getSelf().getUserId();
        List<PublicMessage.Resource> listImages = message.getBody().getImages();


        if (listImages != null && listImages.size() > 0) {
            banner.setVisibility(View.VISIBLE);


            for (int i = 0; i < listImages.size(); i++) {
                String url = listImages.get(i).getOriginalUrl();
                imgesUrl.add(url);
            }

            banner.setData(imgesUrl, null);//第二个参数为提示文字资源集合
            banner.setmAdapter(new XBanner.XBannerAdapter() {
                @Override
                public void loadBanner(XBanner banner, Object model, View view, int position) {
                    GlideImageUtils.setImageView(CircleDetailActivity.this, imgesUrl.get(position), (ImageView) view);
                }
            });
        } else {
            banner.setVisibility(View.GONE);

        }

        /* 设置头像 */
        AvatarHelper.getInstance().displayAvatar(message.getUserId(), avatar_img);
        nick_name_tv.setText(message.getNickName());
        time_distance.setText(TimeUtils.getFriendlyTimeDesc(mContext, (int) message.getTime()));
        tv_content.setText(message.getBody().getText());
        tv_read_count.setText(String.valueOf(message.getCount().getTotal()));
        tvThumb.setText(String.valueOf(message.getPraise()));
        ivThumb.setChecked(1 == message.getIsPraise());

        adapter = new CommentAdapter(data);
        command_listView.setAdapter(adapter);
    }


    private void initActionBar() {
        getSupportActionBar().hide();


        tv_title_center.setText("评论详情");


    }

    public void onBindLinstener() {

        iv_title_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        llThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 是否是点过赞，
                final boolean isPraise = ivThumb.isChecked();
                // 调接口，旧代码保留，传的是相反的状态，
                praiseOrCancel(!isPraise);
                // 更新赞数，调接口完成还会刷新，
                int praiseCount = message.getPraise();
                if (isPraise) {
                    praiseCount--;
                } else {
                    praiseCount++;
                }
                tvThumb.setText(String.valueOf(praiseCount));
                ivThumb.toggle();
            }
        });
        tv_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(CircleDetailActivity.this, getString(R.string.enter_pinlunt),
                        str -> {
                            Comment mComment = new Comment();
                            Comment comment = mComment.clone();
                            if (comment == null)
                                comment = new Comment();
                            comment.setBody(str);
                            comment.setUserId(mUserId);
                            comment.setNickName(mUserName);
                            comment.setTime(TimeUtils.sk_time_current_time());
                            addComment( comment);
                        });
                Window window = trillCommentInputDialog.getWindow();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                    trillCommentInputDialog.show();
                }
            }
        });

    }

    /**
     * 赞 || 取消赞
     */
    private void praiseOrCancel(final boolean isPraise) {
        if (message == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        String requestUrl;
        if (isPraise) {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_ADD;
        } else {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_DELETE;
        }
        HttpUtils.get().url(requestUrl)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            message.setIsPraise(isPraise ? 1 : 0);
                            List<Praise> praises = message.getPraises();
                            if (praises == null) {
                                praises = new ArrayList<>();
                                message.setPraises(praises);
                            }
                            int praiseCount = message.getPraise();
                            if (isPraise) {
                                // 代表我点赞
                                // 消息实体的改变
                                Praise praise = new Praise();
                                praise.setUserId(mLoginUserId);
                                praises.add(praise);// 不建议将其添加到第一位，否则赞与取消赞的操作会造成点赞名单闪烁的现象
                                praiseCount++;
                                message.setPraise(praiseCount);
                            } else {
                                // 取消我的赞
                                // 消息实体的改变
                                for (int i = 0; i < praises.size(); i++) {
                                    if (mLoginUserId.equals(praises.get(i).getUserId())) {
                                        praises.remove(i);
                                        praiseCount--;
                                        message.setPraise(praiseCount);
                                        break;
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }


    private void loadCommentsNextPage(String messageId) {
        // isLoading同时有noMore效果，只有加载出新数据时才设置isLoading为false,
        if (adapter.isLoading()) {
            return;
        }
        adapter.setLoading(true);
        // 只能是20， 因为朋友圈消息列表接口直接返回了的是第一页20条，
        int pageSize = 20;
        // 有20个就加载第二页也就是index==1, 21个是加载第三页，得到空列表，就能停止了，
        int index = (adapter.getCount() + (pageSize - 1)) / pageSize;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(index));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("messageId", messageId);

        String url = coreManager.getConfig().MSG_COMMENT_LIST;

        tvLoadMore.setTag(messageId);
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new ListCallback<Comment>(Comment.class) {
                    @Override
                    public void onResponse(ArrayResult<Comment> result) {
                        List<Comment> data = result.getData();
                        if (data.size() > 0) {
                            adapter.addAll(data);
                            adapter.setLoading(false);
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_no_more);
                            if (tvLoadMore.getTag() == messageId) {
                                // 隐藏加载按钮，
                                tvLoadMore.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Reporter.post("评论分页加载失败，", e);
                        ToastUtil.showToast(mContext, mContext.getString(R.string.tip_comment_load_error));
                    }
                });

    }

    public void setCommentAdapter(List<Comment> data) {
        if (adapter == null) {
            adapter = new CommentAdapter(data);
            command_listView.setAdapter(adapter);

        } else {
            adapter.notifyDataSetChanged();
        }
        adapter.setLoading(false);
    }

    public class CommentAdapter extends BaseAdapter {
        private int messagePosition;
        private boolean loading;
        private List<Comment> datas;

        CommentAdapter(List<Comment> data) {
            if (data == null) {
                datas = new ArrayList<>();
            } else {
                this.datas = data;
            }
        }


        public void addAll(List<Comment> data) {
            this.datas.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CommentViewHolder holder;
            if (convertView == null) {
                holder = new CommentViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.p_msg_comment_list_item, null);
                holder.text_view = (TextView) convertView.findViewById(R.id.text_view);
                convertView.setTag(holder);
            } else {
                holder = (CommentViewHolder) convertView.getTag();
            }
            final Comment comment = datas.get(position);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            String showName = getShowName(comment.getUserId(), comment.getNickName());
            UserClickableSpan.setClickableSpan(mContext, builder, showName, comment.getUserId());            // 设置评论者的ClickSpanned
            if (!TextUtils.isEmpty(comment.getToUserId()) && !TextUtils.isEmpty(comment.getToNickname())) {
                builder.append(mContext.getString(R.string.replay_infix_comment));
                String toShowName = getShowName(comment.getToUserId(), comment.getToNickname());
                UserClickableSpan.setClickableSpan(mContext, builder, toShowName, comment.getToUserId());// 设置被评论者的ClickSpanned
            }
            builder.append(":");
            // 设置评论内容
            String commentBody = comment.getBody();
            if (!TextUtils.isEmpty(commentBody)) {
                commentBody = StringUtils.replaceSpecialChar(comment.getBody());
                CharSequence charSequence = HtmlUtils.transform200SpanString(commentBody, true);
                builder.append(charSequence);
            }
            holder.text_view.setText(builder);
            holder.text_view.setLinksClickable(true);
            holder.text_view.setMovementMethod(LinkMovementClickMethod.getInstance());

            holder.text_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (comment.getUserId().equals(mLoginUserId)) {
                        // 如果消息是我发的，那么就弹出删除和复制的对话框
                        showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    } else {
                        // 弹出回复的框
                        String toShowName = getShowName(comment.getUserId(), comment.getNickName());
                        if (mContext instanceof BusinessCircleActivity) {
                            ((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, comment.getUserId(), comment.getNickName(), toShowName);
                        } else {
                            EventBus.getDefault().post(new MessageEventReply("Reply", comment, messagePosition, toShowName,
                                    (ListView) parent));
                        }
                    }
                }
            });

            holder.text_view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    return true;
                }
            });

            return convertView;
        }

        public boolean isLoading() {
            return loading;
        }

        public void setLoading(boolean loading) {
            this.loading = loading;
        }

        public void addComment(Comment comment) {
            this.datas.add(0, comment);
            notifyDataSetChanged();
        }
    }

    static class CommentViewHolder {
        TextView text_view;
    }

    /**
     * 缓存getShowName，
     * 每次约两三毫秒，
     */
    private WeakHashMap<String, String> showNameCache = new WeakHashMap<>();

    private String getShowName(String userId, String defaultName) {
        String cache = showNameCache.get(userId);
        if (!TextUtils.isEmpty(cache)) {
            return cache;
        }
        String showName = "";

        if (userId.equals(mLoginUserId)) {
            showName = coreManager.getSelf().getNickName();
        } else {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
            if (friend != null) {
                showName = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            }
        }

        if (TextUtils.isEmpty(showName)) {
            showName = defaultName;
        }
        showNameCache.put(userId, showName);
        return showName;
    }


    private void showCommentLongClickDialog(final int messagePosition, final int commentPosition, final CommentAdapter adapter) {


        if (data == null) {
            return;
        }
        if (commentPosition < 0 || commentPosition >= data.size()) {
            return;
        }
        final Comment comment = data.get(commentPosition);

        CharSequence[] items;
        if (comment.getUserId().equals(mLoginUserId)) {
            // 我的评论 || 我的消息，那么我就可以删除
            items = new CharSequence[]{mContext.getString(R.string.copy), mContext.getString(R.string.delete)};
        } else {
            items = new CharSequence[]{mContext.getString(R.string.copy)};
        }
        new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 复制文字
                        if (TextUtils.isEmpty(comment.getBody())) {
                            return;
                        }
                        SystemUtil.copyText(mContext, comment.getBody());
                        break;
                    case 1:
                        deleteComment(message, messagePosition, comment.getCommentId(), data, commentPosition, adapter);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    /**
     * 删除一条回复
     */
    private void deleteComment(PublicMessage message, int messagePosition, String commentId, final List<Comment> comments, final int commentPosition, final CommentAdapter adapter) {
        String messageId = message.getMessageId();
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        params.put("commentId", commentId);
        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).MSG_COMMENT_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            message.setCommnet(message.getCommnet() - 1);
                            comments.remove(commentPosition);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }


    private void addComment( final Comment comment) {
        String messageId = message.getMessageId();
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        if (comment.isReplaySomeBody()) {
            params.put("toUserId", comment.getToUserId() + "");
            params.put("toNickname", comment.getToNickname());
            params.put("toBody", comment.getToBody());
        }
        params.put("body", comment.getBody());

        HttpUtils.post().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if ( Result.checkSuccess(CircleDetailActivity.this, result)) {
                            comment.setCommentId(result.getData());
                        tv_read_count.setText(String.valueOf(message.getCount().getTotal()+1));
                            adapter.addComment(comment);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(CircleDetailActivity.this);
                    }
                });
    }
}