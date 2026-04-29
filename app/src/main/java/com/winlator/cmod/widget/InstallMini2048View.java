package com.winlator.cmod.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Minimal 2048 for the system-files install dialog. Swipe to move; disappears with the dialog.
 */
public class InstallMini2048View extends View {
    private static final int SIZE = 4;
    private final int[][] grid = new int[SIZE][SIZE];
    private final Random random = new Random();
    private final Paint paintBoard = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float downX;
    private float downY;
    private final float swipePx;

    public InstallMini2048View(Context context) {
        super(context);
        swipePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, context.getResources().getDisplayMetrics());
        initPaints(context);
        newGame();
    }

    public InstallMini2048View(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        swipePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, context.getResources().getDisplayMetrics());
        initPaints(context);
        newGame();
    }

    private void initPaints(Context context) {
        setClickable(true);
        paintBoard.setColor(0xffcdc1b4);
        paintCell.setAntiAlias(true);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setFakeBoldText(true);
        float ts = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 17f, context.getResources().getDisplayMetrics());
        paintText.setTextSize(ts);
    }

    public void newGame() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = 0;
            }
        }
        addRandomTile();
        addRandomTile();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int s = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 216f, getResources().getDisplayMetrics());
        setMeasuredDimension(resolveSize(s, widthMeasureSpec), resolveSize(s, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float pad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, getResources().getDisplayMetrics());
        float cell = (Math.min(w, h) - pad * (SIZE + 1)) / SIZE;
        float ox = (w - (pad * (SIZE + 1) + cell * SIZE)) * 0.5f + pad;
        float oy = (h - (pad * (SIZE + 1) + cell * SIZE)) * 0.5f + pad;

        canvas.drawRoundRect(new RectF(0, 0, w, h), pad * 2, pad * 2, paintBoard);

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                float x = ox + j * (cell + pad);
                float y = oy + i * (cell + pad);
                RectF r = new RectF(x, y, x + cell, y + cell);
                int v = grid[i][j];
                paintCell.setColor(tileColor(v));
                canvas.drawRoundRect(r, pad, pad, paintCell);
                if (v != 0) {
                    paintText.setColor(v <= 4 ? 0xff776e65 : 0xfff9f6f2);
                    float cx = r.centerX();
                    float cy = r.centerY() - (paintText.descent() + paintText.ascent()) * 0.5f;
                    canvas.drawText(String.valueOf(v), cx, cy, paintText);
                }
            }
        }
    }

    private static int tileColor(int v) {
        if (v == 0) return 0xffeee4da;
        return switch (v) {
            case 2 -> 0xffeee4da;
            case 4 -> 0xffede0c8;
            case 8 -> 0xfff2b179;
            case 16 -> 0xfff59563;
            case 32 -> 0xfff67c5f;
            case 64 -> 0xfff65e3b;
            default -> 0xffedc22e;
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                boolean moved = false;
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > swipePx) moved = moveRight();
                    else if (dx < -swipePx) moved = moveLeft();
                } else {
                    if (dy > swipePx) moved = moveDown();
                    else if (dy < -swipePx) moved = moveUp();
                }
                if (moved) {
                    addRandomTile();
                    invalidate();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private static int[] mergeLine(int[] row) {
        int[] tiles = new int[SIZE];
        int n = 0;
        for (int v : row) {
            if (v != 0) tiles[n++] = v;
        }
        int[] out = new int[SIZE];
        int o = 0;
        for (int i = 0; i < n; i++) {
            if (i < n - 1 && tiles[i] == tiles[i + 1]) {
                out[o++] = tiles[i] * 2;
                i++;
            } else {
                out[o++] = tiles[i];
            }
        }
        return out;
    }

    private static boolean rowsDiffer(int[] a, int[] b) {
        for (int i = 0; i < SIZE; i++) {
            if (a[i] != b[i]) return true;
        }
        return false;
    }

    private boolean moveLeft() {
        boolean changed = false;
        for (int i = 0; i < SIZE; i++) {
            int[] before = new int[]{grid[i][0], grid[i][1], grid[i][2], grid[i][3]};
            int[] after = mergeLine(before);
            if (rowsDiffer(before, after)) changed = true;
            System.arraycopy(after, 0, grid[i], 0, SIZE);
        }
        return changed;
    }

    private boolean moveRight() {
        boolean changed = false;
        for (int i = 0; i < SIZE; i++) {
            int[] before = new int[]{grid[i][3], grid[i][2], grid[i][1], grid[i][0]};
            int[] after = mergeLine(before);
            if (rowsDiffer(before, after)) changed = true;
            grid[i][0] = after[3];
            grid[i][1] = after[2];
            grid[i][2] = after[1];
            grid[i][3] = after[0];
        }
        return changed;
    }

    private boolean moveUp() {
        boolean changed = false;
        for (int j = 0; j < SIZE; j++) {
            int[] before = new int[]{grid[0][j], grid[1][j], grid[2][j], grid[3][j]};
            int[] after = mergeLine(before);
            if (rowsDiffer(before, after)) changed = true;
            for (int i = 0; i < SIZE; i++) grid[i][j] = after[i];
        }
        return changed;
    }

    private boolean moveDown() {
        boolean changed = false;
        for (int j = 0; j < SIZE; j++) {
            int[] before = new int[]{grid[3][j], grid[2][j], grid[1][j], grid[0][j]};
            int[] after = mergeLine(before);
            if (rowsDiffer(before, after)) changed = true;
            grid[0][j] = after[3];
            grid[1][j] = after[2];
            grid[2][j] = after[1];
            grid[3][j] = after[0];
        }
        return changed;
    }

    private void addRandomTile() {
        int empty = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == 0) empty++;
            }
        }
        if (empty == 0) return;
        int pick = random.nextInt(empty);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == 0) {
                    if (pick == 0) {
                        grid[i][j] = random.nextFloat() < 0.9f ? 2 : 4;
                        return;
                    }
                    pick--;
                }
            }
        }
    }
}
