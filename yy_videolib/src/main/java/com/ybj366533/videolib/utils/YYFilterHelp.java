package com.ybj366533.videolib.utils;

import com.ybj366533.gtvimage.gtvfilter.filter.instagram.FilterHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ken on 2018/4/15.
 */

public class YYFilterHelp {

    public class GTVFilterGroupInfo{
        int groupIndex;
        String groupName;
    }
    static public class GTVFilterInfo {
        int filterIndex;
        String filterName;
        String filterTitle;
        int fillterGroup;

        public GTVFilterInfo(int filterIndex, String filterName, String filterTitle, int fillterGroup) {
            this.filterIndex = filterIndex;
            this.filterName = filterName;
            this.filterTitle = filterTitle;
            this.fillterGroup = fillterGroup;
        }

        public int getFilterIndex() {
            return filterIndex;
        }

        public String getFilterName() {
            return filterName;
        }

        public String getFilterTitle() {
            return filterTitle;
        }

        public int getFillterGroup() {
            return fillterGroup;
        }

        public void setFilterIndex(int filterIndex) {
            this.filterIndex = filterIndex;
        }

        public void setFilterName(String filterName) {
            this.filterName = filterName;
        }

        public void setFilterTitle(String filterTitle) {
            this.filterTitle = filterTitle;
        }

        public void setFillterGroup(int fillterGroup) {
            this.fillterGroup = fillterGroup;
        }
    }

    public static List<GTVFilterInfo> getFilterList(){

        List<GTVFilterInfo> list = new ArrayList<GTVFilterInfo>();
        String[] filterNameList = FilterHelper.getGTVFilterNameList();
        String[] filterTitleList = FilterHelper.getGTVFilterTitleList();
        for(int i = 0; i < filterNameList.length; ++i) {
            list.add(new GTVFilterInfo(i,filterNameList[i], filterTitleList[i],0));
        }
        return list;
    }
}
