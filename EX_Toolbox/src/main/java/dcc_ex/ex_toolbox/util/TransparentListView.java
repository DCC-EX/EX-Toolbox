package dcc_ex.ex_toolbox.util;

/*  override the overscrollfooter to insure it is transparent.  Needed for listviews which do not use all 
 *    available space.  Popular suggestion of warp_content causes multiple "loads" of the listview items,
 *    which is not good for items which include web images (was retrieving multiple times)
 *    Found this solution here: http://stackoverflow.com/a/7974508
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** @noinspection CallToPrintStackTrace*/
public class TransparentListView extends ListView {

    private void makeTransparent() {
        try {

            Method overscrollFooterMethod =
                    TransparentListView.class.getMethod("setOverscrollFooter", Drawable.class);
            Method overscrollHeaderMethod =
                    TransparentListView.class.getMethod("setOverscrollHeader", Drawable.class);


            try {
                overscrollFooterMethod.invoke(this, new Object[]{null});
                overscrollHeaderMethod.invoke(this, new Object[]{null});
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (SecurityException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public TransparentListView(Context context) {
        super(context);
        this.makeTransparent();
    }

    public TransparentListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.makeTransparent();
    }

    public TransparentListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.makeTransparent();
    }
}