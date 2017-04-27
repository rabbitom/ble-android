package net.erabbit.ble.dialog;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.erabbit.ble.R;

/**
 * Created by ziv on 17/3/17.
 */

public class GeneralAlertDialog extends BaseDialog {
    private LinearLayout operateLayout, rightLayout;
    private Button leftBtn, rightBtn;
    private TextView titleText, contentText;


    public GeneralAlertDialog(Context context) {
        super(context);
    }


    public GeneralAlertDialog(Context context, int theme) {
        super(context, theme);
    }

    //回调这个方法啦
    @Override
    public int getLayoutId() {
        return R.layout.alert_dialog_general;
    }

    //也回调了父类的init，利用getLayoutId传入了布局的id
    @Override
    protected void init() {
        super.init();
        operateLayout = (LinearLayout) view.findViewById(R.id.operateLayout);
        rightLayout = (LinearLayout) view.findViewById(R.id.rightLayout);
        leftBtn = (Button) view.findViewById(R.id.leftBtn);
        rightBtn = (Button) view.findViewById(R.id.rightBtn);
        titleText = (TextView) view.findViewById(R.id.title);
        contentText = (TextView) view.findViewById(R.id.content);
    }

    public GeneralAlertDialog setTitle(String str) {
        titleText.setText(str);
        return this;
    }

    public GeneralAlertDialog setContent(String str) {
        contentText.setText(str);
        return this;
    }

    public GeneralAlertDialog setOnLeftBtnClickListen(View.OnClickListener onLeftOnClickListen) {
        leftBtn.setOnClickListener(onLeftOnClickListen);
        return this;
    }

    public GeneralAlertDialog setOnRightBtnClickListen(View.OnClickListener onRightOnClickListen) {
        leftBtn.setOnClickListener(onRightOnClickListen);
        return this;
    }

    public void showDialog(String title, String context, String leftText, String rightText, View.OnClickListener onLeftBtnClickListener, View.OnClickListener onRightBtnClickListener) {
        titleText.setText(title);
        contentText.setText(context);
        leftBtn.setText(leftText);
        rightBtn.setText(rightText);
        leftBtn.setOnClickListener(onLeftBtnClickListener);
        rightBtn.setOnClickListener(onRightBtnClickListener);
        this.show();
    }

    public void showDialogNoOperate(String title, String context) {
        titleText.setText(title);
        contentText.setText(context);
        operateLayout.setVisibility(View.GONE);
        this.show();
    }

    public void dismissDialog() {
        this.dismiss();
    }

}
