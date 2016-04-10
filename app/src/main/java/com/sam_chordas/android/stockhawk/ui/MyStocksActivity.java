package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,SharedPreferences.OnSharedPreferenceChangeListener{

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  boolean isConnected;

    RecyclerView recyclerView;



    //To show toast message when recycler view already contains old data and update empty text view when recycler view is empty.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Cursor cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,new String[]{QuoteColumns.SYMBOL},QuoteColumns.ISCURRENT + " = ?",new String[]{"1"},null);

        if(key.equals(getString(R.string.stock_state)))
        {

            @StockStatus int state = sharedPreferences.getInt(key, SERVER_DOWN);
           if(cursor.getCount()>0)
         {

            if(state==SERVER_DOWN)
            {

                Toast.makeText(this,getString(R.string.stock_out_dated)+" "+getString(R.string.empty_stock_list_server_down),Toast.LENGTH_LONG).show();

            }
            else if(state==SERVER_INVALID)
            {
                Toast.makeText(this,getString(R.string.stock_out_dated)+" "+getString(R.string.empty_stock_list_server_invalid),Toast.LENGTH_LONG).show();
            }
            else if(state==STOCK_INVALID)
             {
                 Toast.makeText(this,getString(R.string.empty_stock_list_stock_invalid),Toast.LENGTH_LONG).show();
             }

        }

            else{

               updateEmptyView();

            if(state==STOCK_INVALID)
            {
                Toast.makeText(this,getString(R.string.empty_stock_list_stock_invalid),Toast.LENGTH_LONG).show();
            }

        }


      }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SERVER_DOWN,SERVER_INVALID,STOCK_INVALID})
    public @interface StockStatus {}

    public static final int SERVER_DOWN = 1;
    public static final int SERVER_INVALID = 2;
    public static final int STOCK_INVALID =3 ;


    @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    isConnected = activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();
    setContentView(R.layout.activity_my_stocks);
    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null){
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){
        startService(mServiceIntent);
      } else{
        networkToast();
      }
    }
     recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override
              public void onItemClick(View v, int position) {
                //TODO:
                // do something on item click
               if(isConnected) {
                   Intent intent = new Intent(mContext, DetailgraphActivity.class);

                   TextView symbol = (TextView) v.findViewById(R.id.stock_symbol);

                   intent.putExtra(Intent.EXTRA_TEXT, symbol.getText());

                   startActivity(intent);

               }

                  else
               {
                   networkToast();
               }
              }
            }));
    recyclerView.setAdapter(mCursorAdapter);


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (isConnected){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { input.toString() }, null);
                  if (c.getCount() != 0) {
                    Toast toast =
                        Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                    toast.show();
                    return;
                  } else {
                    // Add the stock to DB
                      if(input.toString().length()!=0 ) {
                          mServiceIntent.putExtra("tag", "add");
                          mServiceIntent.putExtra("symbol", input.toString());
                          startService(mServiceIntent);
                      }
                  }
                }
              })
              .show();
        } else {
          networkToast();
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);




    mTitle = getTitle();
    if (isConnected){
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }
  }


  @Override
  public void onResume() {

      super.onResume();
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
      sp.registerOnSharedPreferenceChangeListener(this);

      getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
        QuoteColumns.ISCURRENT + " = ?",
        new String[]{"1"},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
      updateEmptyView();

  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }


   public  void updateEmptyView()
    {


        TextView emptytextview = (TextView) findViewById(R.id.recyclerview_empty);
        if(mCursorAdapter.getItemCount()==0)
        {
            recyclerView.setVisibility(View.GONE);
            emptytextview.setVisibility(View.VISIBLE);
            @StockStatus int stock_state = PreferenceManager.getDefaultSharedPreferences(this).getInt(getString(R.string.stock_state),SERVER_DOWN);
            if(!isConnected)
            {
                emptytextview.setText(getString(R.string.empty_stock_list_notconnected));
            }
            else if(stock_state==SERVER_DOWN)
            {
               emptytextview.setText(getString(R.string.empty_stock_list_server_down));
            }
            else if(stock_state==SERVER_INVALID)
            {
                emptytextview.setText(getString(R.string.empty_stock_list_server_invalid));
            }


        }

        else {

            recyclerView.setVisibility(View.VISIBLE);
            emptytextview.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.remove(getString(R.string.stock_state));
        editor.apply();
    }
}
