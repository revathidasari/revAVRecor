package com.example.avrecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class HorizontalCards extends AppCompatActivity {

    public RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horizontal_cards);

        recyclerView = findViewById(R.id.recycler);

        DataObject[] dataObjectArrayList = new DataObject[]{
                new DataObject("one"),
                new DataObject("2"),
                new DataObject("3"),
                new DataObject("4"),
                new DataObject("5"),
                new DataObject("6"),
                new DataObject("7")
        };
    /*    for (int i=0; i<7; i++) {
            dataObjectArrayList += new DataObject[] {new DataObject("check"+i)};
                    //.add(i,"check"+i);
        }*/
        Log.d("revathi","size "+dataObjectArrayList.length);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        CustomAdapter customAdapter = new CustomAdapter(dataObjectArrayList);
        recyclerView.setAdapter(customAdapter);
        recyclerView.addItemDecoration(new CirclePageIndicatorDecoration());


    }
}