package com.example.sharedbikeclient.ui.charity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sharedbikeclient.R;

import java.util.ArrayList;
import java.util.List;

public class CharityFragment extends Fragment {

    private ViewPager2 viewPager;
    private CharityAdapter adapter;
    private List<Charity> charityList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_charity, container, false);

        viewPager = view.findViewById(R.id.viewPager);
        charityList = new ArrayList<>();
        initCharityData();

        adapter = new CharityAdapter(getContext(), charityList);
        viewPager.setAdapter(adapter);

        return view;
    }

    private void initCharityData() {
        charityList.add(new Charity("赵女孩", "女", "1993-05-05", "80CM", "1994-05-01", "新疆维吾尔自治区,昌吉回族自治州,昌吉市,园艺场四队", "现在收养女儿的夫妇应该在70多岁了，男的是一个廋形人，女的的眼睛有问题", "https://baobeihuijia.com/bbhj/special/2024/5/s_e1b7ee7044240cc5.jpg"));
        charityList.add(new Charity("侯天佑", "男", "2017-03-13", "120CM", "2024-05-11", "陕西省,宝鸡市,渭滨区,姜谭路河提桥下面", "孩子身高一米二左右，孩子偏瘦，眼睛视力不好，07年在北京做过三次手术双眼视网膜脱落，性格有些自闭似的，倒是脑袋也聪明，像背诗词啥的听几遍也能会背出来，语言普通生活方便沟通还好。", "https://baobeihuijia.com/bbhj/special/2024/6/s_e4c6f27ff5b439ba.jpg"));
        // 添加更多数据
        charityList.add(new Charity("张瑞馨", "女", "2022-10-30", "未知CM", "2022-10-30", "河南省,开封市,通许县", "捡到时有一个奶瓶和一张纸上面写有希望好心人收养", "https://baobeihuijia.com/bbhj/special/2024/5/s_f5a1bbfefb63b771.jpg"));
        charityList.add(new Charity("郑海岸", "男", "1999-07-20", "未知CM", "2000-08-15", "浙江省,温州市,鹿城区,：浙江省温州市鹿城区旺增桥上田", "暂无", "https://baobeihuijia.com/bbhj/special/2024/5/s_129142fdb7e5fd7b.jpg"));
        charityList.add(new Charity("彭得元", "男", "2021-01-09", "50CM", "2021-01-09", "河北省,廊坊市,安次区,码头镇史庄村", "身高50厘米，单眼皮，短头发", "https://baobeihuijia.com/bbhj/special/2024/4/s_c5f0baf081ca9803.jpg"));
    }
}
