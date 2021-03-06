package com.quasma.android.bustrip.service;

import java.util.ArrayList;
import java.util.List;

import com.quasma.android.bustrip.providers.DirectionProviderContract.DirectionTable;
import com.quasma.android.bustrip.rest.DirectionRestMethod;
import com.quasma.android.bustrip.rest.RestMethodResult;
import com.quasma.android.bustrip.rest.resource.Direction;
import com.quasma.android.bustrip.rest.resource.DirectionList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class DirectionProcessor
{
	protected static final String TAG = DirectionProcessor.class.getSimpleName();

	private Context context;
	private String route;

	public DirectionProcessor(Context context, String route) 
	{
		this.context = context;
		this.route = route;
	}

	
	void getDirections(ProcessorCallback callback) 
	{
		RestMethodResult<DirectionList> result = new DirectionRestMethod(context, route).execute();
		if (result.getStatusCode() < 300)
			updateContentProvider(result);
		
		Bundle bundle = new Bundle();
		bundle.putString(NexTripServiceHelper.EXTRA_RESULT_MSG, result.getStatusMsg());
		callback.send(result.getStatusCode(), bundle);
	}

	private void updateContentProvider(RestMethodResult<DirectionList> result) 
	{				
		DirectionList directionList = result.getResource();
		List<Direction> directions = directionList.getDirections();
		ContentResolver cr = this.context.getContentResolver();

		ArrayList<ContentValues> values = new ArrayList<ContentValues>(directions.size());
		for (Direction direction : directions) 
			values.add(direction.toContentValues(result.getTime()));

		int created = cr.bulkInsert(DirectionTable.CONTENT_URI, values.toArray(new ContentValues[0]));
		int deleted = cr.delete(DirectionTable.CONTENT_URI, DirectionTable.ROUTE + " LIKE ? AND " + DirectionTable.CREATED + " <> ?", new String[] { route, "" + result.getTime()});
		
		Log.d(getClass().getSimpleName(), "Created " + created + " Directions");
		Log.d(getClass().getSimpleName(), "Deleted " + deleted + " Directions");
	}
}
