package com.vgaw.rongyundemo.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vgaw.rongyundemo.http.HttpCat;
import com.vgaw.rongyundemo.message.MatchEngine;
import com.vgaw.rongyundemo.R;
import com.vgaw.rongyundemo.message.SystemMessage;
import com.vgaw.rongyundemo.protopojo.FlyCatProto;
import com.vgaw.rongyundemo.util.DataFactory;
import com.vgaw.rongyundemo.view.MyToast;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.TextMessage;

/**
 * Created by caojin on 15-10-21.
 */
public class ConversationActivity extends BaseActivity {
    /**
     * 目标 Id
     */
    private String mTargetId;

    /**
     * 刚刚创建完讨论组后获得讨论组的id 为targetIds，需要根据 为targetIds 获取 targetId
     */
    private String mTargetIds;

    /**
     * 会话类型
     */
    private Conversation.ConversationType mConversationType;

    private boolean isLeaved = false;
    private String anotherName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation);
        Intent intent = getIntent();
        anotherName = intent.getData().getQueryParameter("title");
        ((TextView) findViewById(R.id.tv_title)).setText(anotherName);
        final RelativeLayout rl_back = (RelativeLayout) findViewById(R.id.rl_back);
        rl_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmQuit();
            }
        });
        final RelativeLayout rl_more = (RelativeLayout) findViewById(R.id.rl_more);
        rl_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(v, "添加好友", "查看信息");
            }
        });
        MatchEngine.getInstance().setOnUserLeavedListener(new MatchEngine.OnUserLeavedListener() {
            @Override
            public void onUserLeaved() {
                if (!isLeaved) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final AlertDialog alertDialog = new AlertDialog.Builder(ConversationActivity.this)
                                    .setMessage("对方已离开聊天室，将在3秒后退出")
                                    .setPositiveButton("确认(3s)", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            MatchEngine.getInstance().initialStatus();
                                            dialog.dismiss();
                                            finish();
                                        }
                                    })
                                    .create();
                            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(final DialogInterface dialog) {
                                    final Button btn_positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                                    ValueAnimator animator = ValueAnimator.ofInt(3, 0);
                                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            long current = 3 - (animation.getCurrentPlayTime() / 1000);
                                            if (current == -1){
                                                MatchEngine.getInstance().initialStatus();
                                                dialog.dismiss();
                                                finish();
                                            }else {
                                                btn_positive.setText("确认(" + String.valueOf(current) + "s)");
                                            }
                                        }
                                    });
                                    animator.setDuration(4000);
                                    animator.start();
                                }
                            });
                            alertDialog.setCancelable(false);
                            alertDialog.show();
                        }
                    });
                }
            }
        });
        getIntentDate(intent);
    }

    /**
     * 展示如何从 Intent 中得到 融云会话页面传递的 Uri
     */
    private void getIntentDate(Intent intent) {

        mTargetId = intent.getData().getQueryParameter("targetId");
        mTargetIds = intent.getData().getQueryParameter("targetIds");
        //intent.getData().getLastPathSegment();//获得当前会话类型
        mConversationType = Conversation.ConversationType.valueOf(intent.getData().getLastPathSegment().toUpperCase(Locale.getDefault()));

        enterFragment(mConversationType, mTargetId);
    }

    /**
     * 加载会话页面 ConversationFragment
     *
     * @param mConversationType 会话类型
     * @param mTargetId         目标 Id
     */
    private void enterFragment(Conversation.ConversationType mConversationType, String mTargetId) {

        ConversationFragment fragment = (ConversationFragment) getSupportFragmentManager().findFragmentById(R.id.conversation);

        Uri uri = Uri.parse("rong://" + getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(mConversationType.getName().toLowerCase())
                .appendQueryParameter("targetId", mTargetId).build();

        fragment.setUri(uri);
    }

    private void showPopupWindow(View anchorView, String first, String second) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popView = inflater.inflate(R.layout.popwindow, null);
        TextView tv_first = ((TextView) popView.findViewById(R.id.tv_first));
        TextView tv_second = ((TextView) popView.findViewById(R.id.tv_second));
        tv_first.setText(first);
        tv_second.setText(second);
        tv_first.setOnClickListener(popClickListener);
        tv_second.setOnClickListener(popClickListener);

        PopupWindow popupWindow = new PopupWindow(popView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth((int)(100 * getResources().getDisplayMetrics().density));
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(anchorView, -popupWindow.getWidth() + anchorView.getWidth() - (int) (7 * getResources().getDisplayMetrics().density), -anchorView.getHeight() + (int) (10 * getResources().getDisplayMetrics().density));
    }

    private View.OnClickListener popClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.tv_first) {
                // 添加好友
                new AlertDialog.Builder(ConversationActivity.this)
                        .setMessage(Html.fromHtml("确定添加 <font color=\"#FF4081\">" + anotherName + "</font> 为好友吗？"))
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                RongIM.getInstance().getRongIMClient().sendMessage(Conversation.ConversationType.PRIVATE, anotherName, new SystemMessage(SystemMessage.INVITE, DataFactory.getInstance().getUsername(), "添加好友"), "", "", new RongIMClient.SendMessageCallback() {
                                    @Override
                                    public void onError(Integer integer, RongIMClient.ErrorCode errorCode) {

                                    }

                                    @Override
                                    public void onSuccess(Integer integer) {
                                        MyToast.makeText(ConversationActivity.this, "邀请已发出，请等待对方接受").show();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
            } else {
                // 查看信息
                Intent intent = new Intent(ConversationActivity.this, MeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("isMe", false);
                bundle.putString("name", anotherName);
                intent.putExtra("data", bundle);
                startActivity(intent);
            }
        }
    };

    @Override
    public void onBackPressed() {
        confirmQuit();
    }

    private void confirmQuit(){
        if (mConversationType != Conversation.ConversationType.CHATROOM){
            finish();
            return;
        }
        new AlertDialog.Builder(ConversationActivity.this)
                .setMessage("确认退出聊天室？")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isLeaved = true;
                        MatchEngine.getInstance().sendLeave();
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }
}
