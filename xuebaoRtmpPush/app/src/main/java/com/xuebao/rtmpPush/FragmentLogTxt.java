package com.xuebao.rtmpPush;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

//娃娃币余额的账单列表-即娃娃币变动列表
/**
 *
 * Created by lijuan on 2016/8/23.
 */
public class FragmentLogTxt extends Fragment {

    TextView tvi = null;
    public static FragmentLogTxt newInstance(int index){
        Bundle bundle = new Bundle();
        bundle.putInt("index", 'A' + index);
        FragmentLogTxt fragment = new FragmentLogTxt();
        fragment.setArguments(bundle);
        return fragment;
    }

    public  void outputInfo(String strTxt)
    {
        if( tvi != null)
        {
            String str_conten = tvi.getText().toString();
            if (tvi.getLineCount() >= 20)
                tvi.setText(strTxt);
            else {
                str_conten += "\r\n";
                str_conten += strTxt;
                tvi.setText(str_conten);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, null);

        tvi = view.findViewById( R.id.txtlog );

        return view;
    }
}
