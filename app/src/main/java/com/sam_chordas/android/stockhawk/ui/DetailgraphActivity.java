package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sam_chordas.android.stockhawk.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Created by e on 04-04-2016.
 */
public class DetailgraphActivity extends AppCompatActivity {


    public LineChart linechart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);



        linechart = (LineChart) findViewById(R.id.linechart);

        linechart.setDescription("Stock Value for past week");
        linechart.setDrawBorders(true);

        XAxis xaxis = linechart.getXAxis();
        xaxis.setEnabled(true);
        xaxis.setDrawAxisLine(true);
        xaxis.setDrawLabels(true);
        xaxis.setAxisLineColor(Color.BLACK);
        xaxis.setAxisLineWidth(4f);
        xaxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yaxis = linechart.getAxisLeft();
        yaxis.setEnabled(true);
        yaxis.setDrawAxisLine(true);
        yaxis.setDrawLabels(true);
        yaxis.setAxisLineColor(Color.BLACK);
        yaxis.setAxisLineWidth(4f);
        yaxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);


    }

    class Stockinfo extends AsyncTask<String, Void, LineData>
    {


        @Override
        protected LineData doInBackground(String... params) {


            Stock stock = null;
            try {

                stock = YahooFinance.get(params[0]);
                Calendar from = Calendar.getInstance();
                Calendar to = Calendar.getInstance();
                from.add(Calendar.DATE, -7); // from 5 years ago
                List<HistoricalQuote> list = stock.getHistory(from, to, Interval.DAILY);

                int i = list.size()-1;

                ArrayList<Entry> entries = new ArrayList<Entry>();


                ArrayList<String> xVals = new ArrayList<String>();

                for(HistoricalQuote quote : list ) // IT IS IN REVERSE ORDER
                {


                    Log.d("closing price", quote.getClose().toString());


                    entries.add(new Entry(Float.parseFloat(quote.getClose().toString()), i));

                    // CREATE NEW CLASS TO HAVE BOTH X VALUES AND Y VALUES AND PASS IT TO ONPOSTEXECUTE

                    SimpleDateFormat format1 = new SimpleDateFormat("MM-dd");

                    String formatted = format1.format(list.get(i).getDate().getTime());

                    xVals.add(formatted);

                    i=i-1;

                }


                LineDataSet setComp1 = new LineDataSet(entries, params[0]); //THESE LINES SHOULD COME IN ONPOST EXECUTE
                setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);

                ArrayList<ILineDataSet> dataSets1 = new ArrayList<ILineDataSet>();
                dataSets1.add(setComp1);

                return new LineData(xVals,dataSets1);



            } catch (IOException e) {
                e.printStackTrace();
            }



            return null;
        }



        @Override
        protected void onPostExecute(LineData lineData) {
            super.onPostExecute(lineData);

            linechart.setData(lineData);
            linechart.invalidate();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();

        Stockinfo stockinfo = new Stockinfo();
        stockinfo.execute(getIntent().getExtras().getString(Intent.EXTRA_TEXT));

    }
}
