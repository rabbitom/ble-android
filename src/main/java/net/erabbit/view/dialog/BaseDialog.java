package net.erabbit.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by ziv on 17/3/17.
 */

public abstract class BaseDialog extends Dialog {

    private Context context;    //下面三个定义的跟上面讲得就是一样的啦
   // private String title;
    protected View view;    //看到这里我们定义的就清楚，我们也是借用view这个父类来引入布局的

    public BaseDialog(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public BaseDialog(Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }


    protected void init() {
        //以view来引入布局
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(getLayoutId(), null);
        this.view = view;
        setContentView(view);
        //设置dialog大小
        Window dialogWindow = getWindow();
        WindowManager manager = ((Activity) context).getWindowManager();
        WindowManager.LayoutParams params = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        dialogWindow.setGravity(Gravity.CENTER);
        Display d = manager.getDefaultDisplay(); // 获取屏幕宽、高度
        params.width = (int) (d.getWidth() * 0.8); // 宽度设置为屏幕的0.65，根据实际情况调整
        dialogWindow.setAttributes(params);
    }


    //可以看到这里定义了一个抽象方法，这个将交由子类去实现
    public abstract int getLayoutId();

}
