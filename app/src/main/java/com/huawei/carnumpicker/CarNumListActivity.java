package com.huawei.carnumpicker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarNumListActivity extends AppCompatActivity {

    private MyAdapter mMyAdapter;
    private List<String> mDataList;
    private String[] srcDatas;
    private EditText mBlackEt;
    private EditText mWhiteEt;
    private EditText mWhite1Et;

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int vid = buttonView.getId();
            if (isChecked) {
                lastCheckChangeData.clear();
                lastCheckChangeData.addAll(mDataList);
            } else {
                replaceListData(lastCheckChangeData);
                return;
            }

            if (vid == R.id.enable_black_cb) {
                List<String> temp = new ArrayList<>();
                String[] split = getSplit(mBlackEt);
                for (int i = 0; i < mDataList.size(); i++) {
                    String str = mDataList.get(i);
                    boolean isBlack = false;
                    for (String blackItem : split) {
                        if (str.contains(blackItem)) {
                            isBlack = true;
                            break;
                        }
                    }
                    if (isBlack) {
                        continue;
                    }
                    temp.add(str);
                }
                replaceListData(temp);
            } else if (vid == R.id.enable_white_cb) {
                List<String> temp = new ArrayList<>();
                String[] split = getSplit(mWhiteEt);
                for (int i = 0; i < mDataList.size(); i++) {
                    String str = mDataList.get(i);
                    boolean isWhite = false;
                    for (String whiteItem : split) {
                        if (str.contains(whiteItem)) {
                            isWhite = true;
                            break;
                        }
                    }
                    if (!isWhite) {
                        continue;
                    }
                    temp.add(str);
                }
                replaceListData(temp);
            } else if (vid == R.id.enable_white1_cb) {
                List<String> temp = new ArrayList<>();
                String[] split = getSplit(mWhite1Et);
                for (int i = 0; i < mDataList.size(); i++) {
                    String str = mDataList.get(i);
                    int count = 0;
                    for (String whiteItem : split) {
                        if (str.contains(whiteItem)) {
                            count++;
                        }
                    }
                    if (count != split.length) {
                        continue;
                    }
                    temp.add(str);
                }
                replaceListData(temp);
            } else if (vid == R.id.enable_aa_cb) {
                List<String> temp = new ArrayList<>();
                for (int i = 0; i < mDataList.size(); i++) {
                    String str = mDataList.get(i);
                    if (!isAaNum(str)) {
                        continue;
                    }
                    temp.add(str);
                }
                replaceListData(temp);
            } else if (vid == R.id.enable_aba_cb) {
                List<String> temp = new ArrayList<>();
                for (int i = 0; i < mDataList.size(); i++) {
                    String str = mDataList.get(i);
                    if (!isAbaNum(str)) {
                        continue;
                    }
                    temp.add(str);
                }
                replaceListData(temp);
            }
        }
    };

    private boolean isAaNum(String carNum) {
        char lastCh = 0;
        for (int i = 0; i < carNum.length(); i++) {
            char ch = carNum.charAt(i);
            if (i == 0) {
                lastCh = ch;
                continue;
            }
            if (ch == lastCh) {
                return true;
            } else {
                lastCh = ch;
            }
        }
        return false;
    }

    private boolean isAbaNum(String carNum) {
        int low = 0;
        int fast = 2;
        while (fast < carNum.length()) {
            if (carNum.charAt(low) == carNum.charAt(fast)) {
                return true;
            }
            low++;
            fast++;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_num_list);

        srcDatas = getIntent().getStringArrayExtra("data");

        findViewById(R.id.show_all_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> temp = new ArrayList<>();
                for (int i = 0; i < mDataList.size(); i++) {
                    String s = mDataList.get(i);
                    if (s.contains("9")) {
                        continue;
                    }
                    temp.add(s);
                }
                int size = mDataList.size();
                mDataList.clear();
                mMyAdapter.notifyItemRangeRemoved(0, size);

                mDataList.addAll(temp);
                mMyAdapter.notifyItemRangeInserted(0, temp.size());
            }
        });

        mBlackEt = (EditText) findViewById(R.id.black_list_et);
        mWhiteEt = (EditText) findViewById(R.id.white_list_et);
        mWhite1Et = (EditText) findViewById(R.id.white1_list_et);

        ((CheckBox) findViewById(R.id.enable_black_cb)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((CheckBox) findViewById(R.id.enable_white_cb)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((CheckBox) findViewById(R.id.enable_white1_cb)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((CheckBox) findViewById(R.id.enable_aa_cb)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((CheckBox) findViewById(R.id.enable_aba_cb)).setOnCheckedChangeListener(onCheckedChangeListener);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mDataList = new ArrayList<>(Arrays.asList(srcDatas));
        mMyAdapter = new MyAdapter(mDataList);
        recyclerView.setAdapter(mMyAdapter);
    }

    private String[] getSplit(EditText blackOrWhite) {
        String whiteList = blackOrWhite.getText().toString().replace("，", ",");
        String[] split;
        if (whiteList.contains(",")) {
            split = whiteList.split(",");
        } else {
            split = new String[]{whiteList};
        }
        return split;
    }

    private void replaceListData(List<String> temp) {
        int size = mDataList.size();
        mDataList.clear();
        mMyAdapter.notifyItemRangeRemoved(0, size);

        mDataList.addAll(temp);
        mMyAdapter.notifyItemRangeInserted(0, temp.size());
    }

    List<String> lastCheckChangeData = new ArrayList<>();

    private void showAllData() {
        List<String> temp = Arrays.asList(srcDatas);

        replaceListData(temp);
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyHolder> {
        private final List<String> datas;

        public MyAdapter(List<String> datas) {
            this.datas = datas;
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.simple_list_item_1, parent, false);
            return new MyHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            holder.text.setText((position + 1 < 10 ? "0" : "") + (position + 1) + "--->" + datas.get(position));
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }
    }
}