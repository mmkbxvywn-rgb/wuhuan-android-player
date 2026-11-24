package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.core.graphics.ColorUtils;

/**
 * 手机点播详情页固定使用无患影音品牌色。
 * 海报只负责展示内容，不再把海报中的红、紫等高饱和颜色带入操作控件和页面底色。
 */
public class VodTheme {

    public static final int NEUTRAL_SEED = 0xFF6F9585;

    private static final int SURFACE = 0xFFF5F7F3;
    private static final int CARD = 0xFFFBFCF9;
    private static final int INK = 0xFF293730;
    private static final int MUTED_INK = 0xFF718078;
    private static final int ACCENT = 0xFF6F9585;
    private static final int ON_ACCENT = 0xFF14251D;
    private static final int OUTLINE = 0x263D5147;

    public final int surface;
    public final int card;
    public final int accent;
    public final int onAccent;
    public final int onSurface;
    public final int onVariant;
    private final boolean glass;

    private VodTheme(int ignoredSeed) {
        glass = false;
        surface = SURFACE;
        card = CARD;
        onSurface = INK;
        onVariant = MUTED_INK;
        accent = ACCENT;
        onAccent = ON_ACCENT;
    }

    /** 记录页等透出壁纸的页面：保持原有透明背景语义。 */
    private VodTheme(boolean glass) {
        this.glass = glass;
        surface = Color.TRANSPARENT;
        card = 0x26000000;
        onSurface = 0xFFF5F2EE;
        onVariant = ColorUtils.setAlphaComponent(onSurface, 0xB3);
        accent = ACCENT;
        onAccent = ON_ACCENT;
    }

    public static VodTheme from(int seed) {
        return new VodTheme(seed);
    }

    public static VodTheme glass() {
        return new VodTheme(true);
    }

    /** 手机浅色记录页：月白卡片、青灰文字与低饱和青绿强调色。 */
    public static VodTheme light() {
        return new VodTheme();
    }

    private VodTheme() {
        glass = true;
        surface = Color.TRANSPARENT;
        card = CARD;
        onSurface = INK;
        onVariant = MUTED_INK;
        accent = ACCENT;
        onAccent = ON_ACCENT;
    }

    public interface Callback {
        void onTheme(VodTheme theme);
    }

    /** 保留原调用接口，但不再从海报提取品牌强调色。 */
    public static void extract(Bitmap bitmap, Callback callback) {
        if (callback != null) callback.onTheme(from(NEUTRAL_SEED));
    }

    /** 圆角卡片：选中时使用低饱和青绿描边。 */
    public GradientDrawable card(boolean selected, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radiusPx);
        d.setColor(selected ? mix(card, accent, 0.12f) : card);
        int stroke = selected ? accent : (glass ? Color.TRANSPARENT : OUTLINE);
        d.setStroke(ResUtil.dp2px(selected ? 2 : 1), stroke);
        return d;
    }

    public GradientDrawable circle(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        return d;
    }

    /** 详情页操作圆钮：浅青灰表面 + 青灰图标。 */
    public GradientDrawable actionCircle() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFFEEF2ED);
        d.setStroke(ResUtil.dp2px(1), OUTLINE);
        return d;
    }

    /** 压在剧照上的悬浮圆钮：浅色半透明底，深色图标保持清晰。 */
    public GradientDrawable floatingCircle() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xE6FBFCF9);
        d.setStroke(ResUtil.dp2px(1), 0x33808F83);
        return d;
    }

    /** 头图底部平滑过渡到月白页面底色。 */
    public GradientDrawable headerScrim() {
        int transparent = ColorUtils.setAlphaComponent(surface, 0);
        int half = ColorUtils.setAlphaComponent(surface, 0x66);
        int mostly = ColorUtils.setAlphaComponent(surface, 0xB3);
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{transparent, half, mostly, surface});
    }

    public static int mix(int a, int b, float t) {
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
