package com.foobnix.pdf.info;

import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

public class OutlineHelper {

    public final static List<Integer> CHAPTER_FORMATS = Arrays.asList(//
            AppState.CHAPTER_FORMAT_1, //
            AppState.CHAPTER_FORMAT_2, //
            AppState.CHAPTER_FORMAT_3 //
    );//

    public final static List<String> CHAPTER_STRINGS = Arrays.asList(//
            "50% 50 / 100 (20)", //
            "50 / 100", //
            "Chapter II " + TxtUtils.LONG_DASH1 + " 3 / 20" //
    );//

    public static class Info {
        public String textPage;
        public String textMax;
        public String chText;
    }

    public static void showChapterFormatPopup(final View v, final Runnable onRefresh) {
        final MyPopupMenu popupMenu = new MyPopupMenu(v.getContext(), v);
        for (int i = 0; i < OutlineHelper.CHAPTER_STRINGS.size(); i++) {
            final int j = i;
            popupMenu.getMenu().add(OutlineHelper.CHAPTER_STRINGS.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().chapterFormat = OutlineHelper.CHAPTER_FORMATS.get(j);
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    return false;
                }
            });
        }
        popupMenu.show();
    }

    public static Info getForamtingInfo(DocumentController dc) {
        Info info = new Info();

        int max = dc.getPageCount();
        String textPage = TxtUtils.deltaPage(dc.getCurentPageFirst1(), max);
        String textMax = TxtUtils.deltaPageMax(max);

        if (AppState.get().isRTL) {
            info.textPage = textPage;
            info.textMax = textMax;
        } else {
            info.textPage = textMax;
            info.textMax = textPage;
        }

        if (AppState.get().chapterFormat == AppState.CHAPTER_FORMAT_1) {
            String text = TxtUtils.getProgressPercent(dc.getCurentPageFirst1(), max) + "   " + TxtUtils.deltaPage(dc.getCurentPageFirst1()) + " ∕ " + textMax;

            int leftPages = getLeftPages(dc);
            text += "   (" + leftPages + ")" + (leftPages < 10 ? "  " : "");

            info.chText = text;
        } else if (AppState.get().chapterFormat == AppState.CHAPTER_FORMAT_2) {
            info.chText = textPage + " ∕ " + textMax;
        } else if (AppState.get().chapterFormat == AppState.CHAPTER_FORMAT_3) {
            OutlineLinkWrapper currentChapter = getCurrentChapter(dc);
            OutlineLinkWrapper nextChapter = getNextChapter(dc);

            if (currentChapter == null) {
                info.chText = textPage + " ∕ " + textMax;
            } else {

                int lastPage = max;
                int pages = max;
                if (nextChapter != null) {
                    lastPage = currentChapter.targetPage;
                    pages = nextChapter.targetPage;
                }
                int pageRel = dc.getCurentPageFirst1() - lastPage + 1;
                int totalChapter = pages - currentChapter.targetPage;
                String currentChapterAsString = getCurrentChapterAsString(dc);
                currentChapterAsString = TxtUtils.substring(currentChapterAsString, 20);
                info.chText = currentChapterAsString + " " + TxtUtils.LONG_DASH1 + " " + pageRel + " / " + totalChapter;
                info.chText += pageRel < 10 ? "  " : "";
            }
        }

        if (AppState.get().isAutoScroll) {
            info.chText = String.format("{%s} %s", AppState.get().autoScrollSpeed, info.chText);
        }

        return info;

    }

    public static int getLeftPages(DocumentController dc) {
        int maxPages = dc.getPageCount();
        int currentPage = dc.getCurentPageFirst1();
        List<OutlineLinkWrapper> dividers = dc.getCurrentOutline();
        if (TxtUtils.isListEmpty(dividers)) {
            return dc.getPageCount();
        }

        int first = dividers.get(0).level;
        for (int i = 0; i < dividers.size(); i++) {
            OutlineLinkWrapper item = dividers.get(i);
            int nextTarget = item.targetPage;
            if (nextTarget > currentPage && item.level <= (AppState.get().isShowSubChaptersOnProgress ? 2 : 0) + first) {
                return nextTarget - currentPage;
            }
        }
        return maxPages - currentPage;
    }

    public static String getCurrentChapterAsString(DocumentController dc) {
        List<OutlineLinkWrapper> outline = dc.getCurrentOutline();

        if (outline == null || outline.isEmpty()) {
            return null;
        }
        int root = getRootItemByPageNumber(dc);
        if (outline.size() > root) {
            OutlineLinkWrapper item = outline.get(root);
            return item.getTitleAsString();
        } else {
            return null;
        }
    }

    public static OutlineLinkWrapper getCurrentChapter(DocumentController dc) {
        List<OutlineLinkWrapper> outline = dc.getCurrentOutline();

        if (outline == null || outline.isEmpty()) {
            return null;
        }
        int root = getRootItemByPageNumber(dc);
        if (outline.size() > root) {
            return outline.get(root);
        } else {
            return null;
        }
    }

    public static OutlineLinkWrapper getNextChapter(DocumentController dc) {
        List<OutlineLinkWrapper> outline = dc.getCurrentOutline();

        if (outline == null || outline.isEmpty()) {
            return null;
        }
        int root = getRootItemByPageNumber(dc) + 1;
        if (outline.size() > root) {
            return outline.get(root);
        } else {
            return null;
        }
    }

    public static int getRootItemByPageNumber(DocumentController dc) {
        try {
            List<OutlineLinkWrapper> outline = dc.getCurrentOutline();
            int pageNumber = dc.getCurentPageFirst1();
            for (int i = 0; i < outline.size(); i++) {
                OutlineLinkWrapper item = outline.get(i);

                if (item.targetPage == pageNumber) {
                    return i;
                } else if (item.targetPage > pageNumber) {
                    return Math.max(0, i - 1);
                }

            }

            return outline.size() - 1;
        } catch (Exception e) {
            return 0;
        }
    }

}
