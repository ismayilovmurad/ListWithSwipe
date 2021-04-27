package com.martiandeveloper.listwithswipe.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.martiandeveloper.listwithswipe.model.Product;
import com.martiandeveloper.listwithswipe.adapter.ProductListAdapter;
import com.martiandeveloper.listwithswipe.R;
import com.martiandeveloper.listwithswipe.tools.RecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView activityMainMainRV;

    private RecyclerViewTouchListener recyclerViewTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityMainMainRV = findViewById(R.id.activity_main_mainRV);

        List<Product> productList = new ArrayList<>();

        for (int i = 1; i < 17; i++) {
            productList.add(new Product("Product " + i));
        }

        activityMainMainRV.setLayoutManager(new LinearLayoutManager(this));
        activityMainMainRV.setAdapter(new ProductListAdapter(productList));

        recyclerViewTouchListener = new RecyclerViewTouchListener(this, activityMainMainRV);

        recyclerViewTouchListener
                .setClickable(new RecyclerViewTouchListener.OnRowClickListener() {
                    @Override
                    public void onRowClicked(int position) {
                        Toast.makeText(getApplicationContext(), productList.get(position).getName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onIndependentViewClicked(int independentViewID, int position) {

                    }
                })
                .setSwipeOptionViews(R.id.recyclerview_product_item_deleteFL, R.id.recyclerview_product_item_editFL)
                .setSwipeable(R.id.recyclerview_product_item_mainLL, R.id.recyclerview_product_item_containerLL, (viewID, position) -> {
                    if (viewID == R.id.recyclerview_product_item_editFL)
                        Toast.makeText(getApplicationContext(), "Edit was clicked", Toast.LENGTH_SHORT).show();
                    else if (viewID == R.id.recyclerview_product_item_deleteFL)
                        Toast.makeText(getApplicationContext(), "Delete was clicked", Toast.LENGTH_SHORT).show();
                });

        activityMainMainRV.addOnItemTouchListener(recyclerViewTouchListener);

    }

    @Override
    public void onResume() {
        super.onResume();
        activityMainMainRV.addOnItemTouchListener(recyclerViewTouchListener);
    }

}
