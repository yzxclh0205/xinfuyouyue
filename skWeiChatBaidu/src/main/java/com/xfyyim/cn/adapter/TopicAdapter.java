package com.xfyyim.cn.adapter;

import android.content.Context;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xfyyim.cn.R;
import com.xfyyim.cn.bean.circle.TopicEntity;
import com.xfyyim.cn.util.TimeUtils;
import com.xfyyim.cn.util.glideUtil.GlideImageUtils;

import java.util.List;

public class TopicAdapter extends BaseQuickAdapter<TopicEntity.DataBean.TopicsBean, BaseViewHolder> {
Context context;

    public TopicAdapter(@Nullable List<TopicEntity.DataBean.TopicsBean> data, Context context) {
        super(R.layout.item_topic,data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, TopicEntity.DataBean.TopicsBean item) {
                helper.setText(R.id.tv_topictime, TimeUtils.getFriendlyTimeDesc(mContext, (int) item.getTime()));
        GlideImageUtils.setImageView(context,item.getBody().getImages().get(0).getOUrl(),helper.getView(R.id.img_topic));

    }
}