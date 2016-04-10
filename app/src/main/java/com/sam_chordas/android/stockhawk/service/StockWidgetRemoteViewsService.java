package com.sam_chordas.android.stockhawk.service;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by e on 07-04-2016.
 */
public class StockWidgetRemoteViewsService extends RemoteViewsService
{
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;
            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {

                if(data!=null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                data= getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = null;

                if(data.getInt(data.getColumnIndex("is_up")) == 1)
                {
                     views = new RemoteViews(getPackageName(), R.layout.widget_list_item_green);

                }
                else
                {
                     views = new RemoteViews(getPackageName(), R.layout.widget_list_item_red);

                }



                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));

                views.setTextViewText(R.id.widget_stock_symbol,symbol);

                String bidprice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));

                views.setTextViewText(R.id.widget_bid_price,bidprice);

                String change = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));

                views.setTextViewText(R.id.widget_change,change);


                if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                {
                    views.setContentDescription(R.id.widget_stock_symbol,getString(R.string.symbol_desc,symbol));
                    views.setContentDescription(R.id.widget_bid_price,getString(R.string.bid_price_desc,bidprice));
                    views.setContentDescription(R.id.widget_change,getString(R.string.change_desc,change));
                }


                final Intent fillintent = new Intent();

                fillintent.putExtra(Intent.EXTRA_TEXT,symbol);

                views.setOnClickFillInIntent(R.id.widget_stock_list_item, fillintent);

                return views;
            }



            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(),R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public long getItemId(int position) {
                if(data.moveToPosition(position))
                {
                    return data.getInt(data.getColumnIndex(QuoteColumns._ID));
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
