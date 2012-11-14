package edu.udo.cs.ess.systemtap;
import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Databackend for the ListView showing all log files for a specific module
 * It gets updated when an user selects a module from the spinner.
 * @author alex
 *
 */
public class LogFileListAdapter extends BaseAdapter
{
	private static final String TAG = LogFileListAdapter.class.getSimpleName();
	
	private ArrayList<File> mData;
	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	
	public LogFileListAdapter(Activity pContext)
	{
		mData = new ArrayList<File>();
		mActivity = pContext;
		mLayoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public boolean isEmpty()
	{
		return mData.isEmpty();
	}
	
	@Override
	public boolean isEnabled(int pPosition)
	{
		if (pPosition < mData.size())
		{
			return true;
		}
		else
		{
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	@Override
	public int getCount()
	{
		return mData.size();
	}

	@Override
	public Object getItem(int pPosition)
	{
		if (pPosition >= mData.size())
		{
			return null;
		}
		else
		{
			return mData.get(pPosition);
		}
	}

	@Override
	public long getItemId(int pPosition)
	{
		return pPosition;
	}
	
	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent)
	{
		View itemView;
		if (pConvertView == null)
		{
			 itemView = mLayoutInflater.inflate(R.layout.file_list_item, pParent, false);
		}
		else
		{
			itemView = pConvertView;
		}
		
		TextView textViewLogFileListName = (TextView)itemView.findViewById(R.id.textViewFileListName);		
		File file = mData.get(pPosition);
		/* Get file name */
		String fileName = file.getName();
		/* Strip off the extension */
		fileName = fileName.substring(0,fileName.lastIndexOf('.'));
		/* extract the date */
		String date = fileName.substring(fileName.indexOf('_') + 1,fileName.lastIndexOf('_'));
		/* extract the time */
		String time = fileName.substring(fileName.lastIndexOf('_') + 1);
		time = time.replace(".", ":");
		textViewLogFileListName.setText(date + " " + time);
		
		return itemView;
	}

	public void setData(File pData[])
	{
		/* Avoid raceconditions accessing mData */
		synchronized (mData)
		{
			mData.clear();
			for (File cur: pData)
			{
				mData.add(cur);
			}
			this.notifyDataSetChanged();
		}
	}
}
