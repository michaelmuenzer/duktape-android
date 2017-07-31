/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.duktape.tests;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.squareup.duktape.Duktape;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import okio.BufferedSource;
import okio.Okio;

public final class OctaneActivity extends Activity {
    private TextView output;
    private View run;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.octane);

        output = (TextView) findViewById(R.id.output);

        run = findViewById(R.id.run);
        run.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new BenchmarkTask().execute();
            }
        });
    }

    // Just don't rotate your phone...
    class BenchmarkTask extends AsyncTask<Void, String, String> {
        @Override
        protected void onPreExecute() {
            run.setEnabled(false);
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder output = new StringBuilder();

            Duktape duktape = Duktape.create(getApplicationContext());
            try {
                for (String file : getAssets().list("octane")) {
                    long tookMs = evaluateAsset(duktape, "octane/" + file);
                    output.append(file).append(" eval took ").append(tookMs).append(" ms\n");
                    publishProgress(output.toString());
                }
                evaluateAsset(duktape, "octane.js");

                String results = (String) duktape.evaluate("getResults();");
                output.append('\n').append(results);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                output.append(sw.toString());
            }

            duktape.close();

            return output.toString();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            output.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String value) {
            output.setText(value);
            run.setEnabled(true);
        }
    }

    private long evaluateAsset(Duktape duktape, String file) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(getAssets().open(file)));
        String script = source.readUtf8();
        source.close();

        long startNanos = System.nanoTime();
        duktape.evaluate(script, file);
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
