package com.quasma.android.bustrip.service;

import java.util.ArrayList;
import java.util.List;

import com.quasma.android.bustrip.providers.StopProviderContract.StopTable;
import com.quasma.android.bustrip.rest.RestMethodResult;
import com.quasma.android.bustrip.rest.StopRestMethod;
import com.quasma.android.bustrip.rest.resource.Stop;
import com.quasma.android.bustrip.rest.resource.StopList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class StopProcessor
{
	protected static final String TAG = StopProcessor.class.getSimpleName();

	private Context context;
	private String route;
	private String direction;

	public StopProcessor(Context context, String route, String direction) 
	{
		this.context = context;
		this.route = route;
		this.direction = direction;
	}

	
	void getStops(ProcessorCallback callback) 
	{
		RestMethodResult<StopList> result = new StopRestMethod(context, route, direction).execute();
		if (result.getStatusCode() < 300)
			updateContentProvider(result);
		
		Bundle bundle = new Bundle();
		bundle.putString(NexTripServiceHelper.EXTRA_RESULT_MSG, result.getStatusMsg());
		callback.send(result.getStatusCode(), bundle);
	}

	private void updateContentProvider(RestMethodResult<StopList> result) 
	{				
		StopList stopList = result.getResource();
		List<Stop> stops = stopList.getStops();
		ContentResolver cr = this.context.getContentResolver();

		ArrayList<ContentValues> values = new ArrayList<ContentValues>(stops.size());
		for (Stop stop : stops) 
			values.add(stop.toContentValues(result.getTime()));

		int created = cr.bulkInsert(StopTable.CONTENT_URI, values.toArray(new ContentValues[0]));
		int deleted = cr.delete(StopTable.CONTENT_URI, StopTable.ROUTE + " LIKE ? AND " 
				+ StopTable.DIRECTION + " LIKE ? AND "+ StopTable.CREATED + " <> ?", new String[] { route, direction, "" + result.getTime()});
		
		Log.d(getClass().getSimpleName(), "Created " + created + " Stops");
		Log.d(getClass().getSimpleName(), "Deleted " + deleted + " Stops");
	}
}
