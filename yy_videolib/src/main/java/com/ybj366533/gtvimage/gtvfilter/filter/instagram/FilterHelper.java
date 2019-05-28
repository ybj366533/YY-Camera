package com.ybj366533.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;

import com.ybj366533.videolib.videoplayer.R;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.lookupfilter.GTVLookupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.lookupfilter.GTVRiXiRenXiangFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageDarkGrayFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageLightGrayFilter;

public class FilterHelper extends GTVImageFilter {

//    static private String[] GTVFilterName={"Normal", "Amaro","Rise","Hudson","XproII",
//            "Sierra","Lomofi","Earlybird", "Sutro", "Toaster",
//            "Brannan", "Inkwell", "Walden", "Hefe", "Valencia",
//            "Nashville", "1977", "LordKelvin",
//    "rixi_fenneng", "rixi_naicha", "rixi_richu", "rixi_roumei",
//    "rixi_tianmei", "rixi_yidou", "rixi_yinghua", "rixi_ziran",
//    "hai_zhufu", "hai_daoguo", "hai_denghou", "hai_fengling",
//        "hai_haifeng", "hai_riji", "hai_wuyu", "hai_xingfu"};

    // rixi_fenneng "日系-粉嫩" _rixi_fenneng  0.468f
    static private String[] GTVFilterName={"Normal",
            "1977", "Hefe", "Inkwell","LordKelvin",
            "Nashville","Earlybird","Valencia","Brannan",
            "rixi_shenlin", "rixi_naicha", "rixi_richu", "rixi_roumei",
            "rixi_tianmei", "rixi_yidou", "rixi_yinghua", "rixi_ziran",
            "hai_zhufu", "hai_daoguo", "hai_denghou", "hai_fengling",
            "hai_haifeng", "hai_riji", "hai_wuyu", "hai_xingfu",
            "Amaro","Rise","Hudson","XproII",
            "Sierra","Lomofi","Sutro", "Toaster",
            "Walden","LightGray","DarkGray"};

    static private String[] GTVFilterTitle= new String[] {"原图",
            "电影-怀旧","电影-经典","电影-水墨", "电影-西部",
            "电影-暮色","电影-荒漠","电影-瓦伦", "电影-布朗",
            "日系-森林", "日系-奶茶", "日系-日出", "日系-柔美",
            "日系-甜美", "日系-伊豆", "日系-樱花", "日系-自然",
            "海-祝福", "海-岛国", "海-等候", "海-风铃",
            "海-海风", "海-日记", "海-物语", "海-幸福",
            "Amaro","Rise","Hudson","XproII",
            "Sierra","Lomofi","Sutro", "Toaster",
            "Walden", "LightGray","DarkGray"};

    /// 内部实现专用
    static private int[] lookupFilterImage = new int[] { R.drawable.f_rixi_senlin, R.drawable.f_rixi_naicha, R.drawable.f_rixi_richu, R.drawable.f_rixi_roumei,
            R.drawable.f_rixi_tianmei, R.drawable.f_rixi_yidou, R.drawable.f_rixi_yinghua, R.drawable.f_rixi_ziran,
            R.drawable.f_hai_zhufu, R.drawable.f_hai_daoguo, R.drawable.f_hai_denghou, R.drawable.f_hai_fengling,
            R.drawable.f_hai_haifeng, R.drawable.f_hai_riji, R.drawable.f_hai_wuyu, R.drawable.f_hai_xingfu};

    static private float[] lookupFilterItensity = new float[]{0.679f, 0.764f, 0.676f, 0.608f,
            0.869f, 0.500f, 0.781f, 0.588f,
            0.808f, 0.841f, 0.895f, 0.837f,
            0.722f, 0.451f, 0.687f, 0.875f
    };
    //---------------

    private FilterHelper() {}

    private static final int FILTER_NUM = 1 + 8 + 8 + 8 + 9 + 2;
    private static GTVInstaFilter[] filters;

    public static GTVImageFilter getFilter(Context context, int index) {
//        if (filters == null) {
//            filters = new GTVInstaFilter[FILTER_NUM];
//        }
//        if(index <0 || index >= FILTER_NUM) {
//            index = 0;
//        }
        GTVImageFilter filter =  new GTVIFNormalFilter(context);

        try {
            switch (index){
                case 0:
                    //filter = new GTVIFNormalFilter(context);
                    filter = new GTVRiXiRenXiangFilter(context);

                    break;
                case 1:
                    filter = new GTVIF1977Filter(context);
                    //filter = new GTVIFAmaroFilter(context);
                    //filter = new GTVRiXiRenXiangFilter(context);
                    break;
                case 2:
                    //filter = new GTVIFRiseFilter(context);
                    filter = new GTVIFHefeFilter(context);
                    break;
                case 3:
                    //filter = new GTVIFHudsonFilter(context);
                    filter = new GTVIFInkwellFilter(context);
                    break;
                case 4:
                    //filter = new GTVIFXproIIFilter(context);
                    filter = new GTVIFLordKelvinFilter(context);
                    break;
                case 5:
                    //filter = new GTVIFSierraFilter(context);
                    filter = new GTVIFNashvilleFilter(context);
                    break;
                case 6:
                    //filter = new GTVIFLomofiFilter(context);
                    filter = new GTVIFEarlybirdFilter(context);
                    break;
                case 7:
                    //filter = new GTVIFEarlybirdFilter(context);
                    filter = new GTVIFValenciaFilter(context);
                    break;
                case 8:
                    //filter = new GTVIFSutroFilter(context);
                    filter = new GTVIFBrannanFilter(context);
                    break;
//                case 9:
//                    filter = new GTVIFToasterFilter(context);
//                    break;
//                case 10:
//                    filter = new GTVIFBrannanFilter(context);
//                    break;
//                case 11:
//                    filter = new GTVIFInkwellFilter(context);
//                    break;
//                case 12:
//                    filter = new GTVIFWaldenFilter(context);
//                    break;
//                case 13:
//                    filter = new GTVIFHefeFilter(context);
//                    break;
//                case 14:
//                    filter = new GTVIFValenciaFilter(context);
//                    break;
//                case 15:
//                    filter = new GTVIFNashvilleFilter(context);
//                    break;
//                case 16:
//                    filter = new GTVIF1977Filter(context);
//                    break;
//                case 17:
//                    filter = new GTVIFLordKelvinFilter(context);
//                    break;
                case 9:    //"日系-粉嫩"
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:

                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    filter = new GTVLookupFilter(context,lookupFilterItensity[index-9], lookupFilterImage[index - 9]);
                            break;
                case 25:
                    filter = new GTVIFAmaroFilter(context);
                    break;
                case 26:
                    filter = new GTVIFRiseFilter(context);
                    break;
                case 27:
                    filter = new GTVIFHudsonFilter(context);
                    break;
                case 28:
                    filter = new GTVIFXproIIFilter(context);
                    break;
                case 29:
                    filter = new GTVIFSierraFilter(context);
                    break;
                case 30:
                    filter = new GTVIFLomofiFilter(context);
                    break;
                case 31:
                    filter = new GTVIFSutroFilter(context);
                    break;
                case 32:
                    filter = new GTVIFToasterFilter(context);
                    break;
                case 33:
                    filter = new GTVIFWaldenFilter(context);
                    break;
                case 34:
                    filter = new GTVImageLightGrayFilter(context);
                    break;
                case 35:
                    filter = new GTVImageDarkGrayFilter(context);
                    break;
                default:
                    filter = new GTVIFNormalFilter(context);

            }
        } catch (Throwable e) {
        }
        return filter;
    }

    public static void destroyFilters() {
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                try {
                    if (filters[i] != null) {
                        filters[i].destroy();
                        filters[i] = null;
                    }
                } catch (Throwable e) {
                }
            }
        }
    }

    public static String getFilterName(int index) {
        if(index <0 || index >= FILTER_NUM) {
            index = 0;
        }
        //String[] strArray={"Normal", "Amaro","Rise","Hudson","XproII","Sierra","Lomofi","Earlybird", "Sutro", "Toaster","Brannan", "Inkwell", "Walden", "Hefe", "Valencia", "Nashville", "1977", "LordKelvin"};
        return GTVFilterName[index];
    }

    public static int getFilterIndex(String filterName) {
        int index = 0;
        //String[] strArray={"Normal", "Amaro","Rise","Hudson","XproII","Sierra","Lomofi","Earlybird", "Sutro", "Toaster","Brannan", "Inkwell", "Walden", "Hefe", "Valencia", "Nashville", "1977", "LordKelvin"};
        for (int i = 0; i < GTVFilterName.length; ++i) {
            if (filterName.compareToIgnoreCase(GTVFilterName[i]) ==0){
                return i;
            }
        }
        return index;
    }

    public static String[] getGTVFilterNameList(){
        return GTVFilterName;
    }

    public static String[] getGTVFilterTitleList(){
        return GTVFilterTitle;
    }

}
