package com.sar2014.autorregistro;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

public class Autorregistrar extends ActionBarActivity {

	private final Calendar TIMESTAMP = Calendar.getInstance();
	private Calendar regTime = TIMESTAMP;
	private int anxiety;

	private DialogFragment timeFragment = new TimePickerFragment();
	private DialogFragment dateFragment = new DatePickerFragment();

	private static final String TIMEPICKERTAG = "com.sar2014.autorregistro.TIMEPICKER";
	private static final String DATEPICKERTAG = "com.sar2014.autorregistro.DATEPICKER";

	private static final int TEST_ID = 9;
	private static final String TEST_ID_NAME = "TestID";
	private static final String TIMESTAMP_NAME = "TimeStamp";

	private static final String RESULTS_NAME = "Results";

	private static final int REG_TIME_ID = 10;
	private static final int ANXIETY_ID = 9;
	private static final int SITUATION_ID = 11;

	private static final String SERVERURL = "http://www.sar2014.esy.es/movil.php";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		((Button) findViewById(R.id.set_time_button)).setText(regTime
				.get(Calendar.HOUR_OF_DAY)
				+ ":"
				+ String.format("%02d", regTime.get(Calendar.MINUTE)));
		((Button) findViewById(R.id.set_date_button)).setText(regTime
				.get(Calendar.DAY_OF_MONTH)
				+ "/"
				+ (regTime.get(Calendar.MONTH) + 1)
				+ "/"
				+ regTime.get(Calendar.YEAR));

		timeFragment.show(getFragmentManager(), TIMEPICKERTAG);
	}

	/** Called when the user clicks on the "change time button */
	public void onChangeTime(View view) {

		timeFragment.show(getFragmentManager(), TIMEPICKERTAG);
	}

	/** Called when the user clicks on the "change time button */
	public void onChangeDate(View view) {

		dateFragment.show(getFragmentManager(), DATEPICKERTAG);
	}

	/** Called when the user clicks on the anxiety scale radio buttons */
	public void onRadioButtonClicked(View view) {

		// Is the button now checked?
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		switch (view.getId()) {
		case R.id.anxiety_1:
			if (checked)
				anxiety = 1;
			break;
		case R.id.anxiety_2:
			if (checked)
				anxiety = 2;
			break;
		case R.id.anxiety_3:
			if (checked)
				anxiety = 3;
			break;
		case R.id.anxiety_4:
			if (checked)
				anxiety = 4;
			break;
		case R.id.anxiety_5:
			if (checked)
				anxiety = 5;
			break;
		case R.id.anxiety_6:
			if (checked)
				anxiety = 6;
			break;
		}
	}

	private String calendarToString(Calendar date) {

		String result = date.get(Calendar.YEAR) + "/"
				+ String.format("%02d", date.get(Calendar.MONTH) + 1) + "/"
				+ String.format("%02d", date.get(Calendar.DAY_OF_MONTH))
				+ " - " + String.format("%02d", date.get(Calendar.HOUR_OF_DAY))
				+ ":" + String.format("%02d", date.get(Calendar.MINUTE));

		return (result);
	}

	/** Called when the user clicks the Send button */
	public void sendMessage(View view) {

		String situation = ((EditText) findViewById(R.id.situation_response))
				.getText().toString();

		String results = REG_TIME_ID + "/" + calendarToString(regTime) + "_"
				+ ANXIETY_ID + "/" + anxiety + "_" + SITUATION_ID + "/"
				+ situation;

		JSONObject postMessage = new JSONObject();

		try {
			postMessage.put(TEST_ID_NAME, TEST_ID);
			postMessage.put(TIMESTAMP_NAME, calendarToString(TIMESTAMP));
			postMessage.put(RESULTS_NAME, results);
		} catch (JSONException e) {

			e.printStackTrace();
		}

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {

			new SendRegisterTask().execute(postMessage.toString());
		} else {

			setContentView(R.layout.activity_main_response);
			((TextView) findViewById(R.id.response_text))
					.setText(R.string.err_conn_unavailable);
		}
	}

	private class TimePickerFragment extends DialogFragment implements
			TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this,
					regTime.get(Calendar.HOUR_OF_DAY),
					regTime.get(Calendar.MINUTE),
					DateFormat.is24HourFormat(getActivity()));
		}

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

			regTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			regTime.set(Calendar.MINUTE, minute);

			((Button) findViewById(R.id.set_time_button)).setText(hourOfDay
					+ ":" + String.format("%02d", minute));
		}
	}

	public class DatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this,
					regTime.get(Calendar.YEAR), regTime.get(Calendar.MONTH),
					regTime.get(Calendar.DAY_OF_MONTH));
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {

			regTime.set(Calendar.YEAR, year);
			regTime.set(Calendar.MONTH, month);
			regTime.set(Calendar.DAY_OF_MONTH, day);

			((Button) findViewById(R.id.set_date_button)).setText(day + "/"
					+ (month + 1) + "/" + year);
		}
	}

	private class SendRegisterTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... message) {

			HttpURLConnection httpConn = null;
			InputStreamReader reader = null;
			OutputStream output = null;

			try {
				byte[] request = message[0].getBytes("UTF-8");

				httpConn = (HttpURLConnection) (new URL(SERVERURL))
						.openConnection();
//				httpConn.setReadTimeout(10000 /* milliseconds */);
//				httpConn.setConnectTimeout(15000 /* milliseconds */);
//				httpConn.setRequestMethod("POST");
//				httpConn.setDoInput(true);
				httpConn.setDoOutput(true);
//				httpConn.setFixedLengthStreamingMode(request.length);

//				httpConn.setRequestProperty("Content-Type",
//						"application/json:charset=utf-8");
				httpConn.setRequestProperty("Content-Type", "application/json");
//				httpConn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
				httpConn.setRequestProperty("Accept", "application/json");
				httpConn.setRequestMethod("POST");

				httpConn.connect();

				output = httpConn.getOutputStream();
				output.write(request);
//				output.flush();

				reader = new InputStreamReader(httpConn.getInputStream());

				char[] response = new char[2048];
				reader.read(response);

				return new String(response);

			} catch (Exception e) {

				e.printStackTrace();
				return null;

			} finally {

				try {
					output.close();
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (httpConn != null) {
					httpConn.disconnect();
				}
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			TextView textView = (TextView) findViewById(R.id.response_text);
			textView.setText(result);
		}
	}
}
