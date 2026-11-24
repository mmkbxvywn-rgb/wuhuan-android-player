package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;

import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.color.MaterialColors;

/**
 * 全 App 弹窗统一表面：颜色从当前 Activity 主题读取。
 * 手机端使用月白表面，TV 端继续使用其自身主题色，避免公共代码强制套用某一端的配色。
 */
public final class DialogGlass {

    private static final int FALLBACK_SURFACE = 0xE61B1E26;
    private static final int FALLBACK_STROKE = 0x33FFFFFF;
    private static final int RADIUS_DP = 16;
    private static final int BLUR_DP = 20;

    private DialogGlass() {
    }

    /** 居中弹窗：使用当前主题的 Surface 与 OutlineVariant。 */
    public static void apply(Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(background(dialog.getContext(), false));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(ResUtil.dp2px(BLUR_DP));
        }
    }

    /** 底部弹窗：沿用当前主题表面，不再无条件覆盖为深色玻璃。 */
    public static void applySheet(View content) {
        if (content == null) return;
        content.setBackground(background(content.getContext(), true));
    }

    private static GradientDrawable background(Context context, boolean bottomSheet) {
        int surface = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorSurface, FALLBACK_SURFACE);
        int stroke = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutlineVariant, FALLBACK_STROKE);
        float radius = ResUtil.dp2px(RADIUS_DP);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(surface);
        if (bottomSheet) {
            drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        } else {
            drawable.setCornerRadius(radius);
        }
        drawable.setStroke(Math.max(1, ResUtil.dp2px(1)), stroke);
        return drawable;
    }
}
