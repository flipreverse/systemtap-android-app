package edu.udo.cs.ess.systemtap;
import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class LogFileListAdapter extends BaseAdapter
{
	private static final String TAG = LogFileListAdapter.class.getSimpleName();
	
	private ArrayList<String> mData;
	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	
	public LogFileListAdapter(Activity pContext)
	{
		mData = new ArrayList<String>();
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
			 itemView = mLayoutInflater.inflate(R.layout.module_list_item, pParent, false);
		}
		else
		{
			itemView = pConvertView;
		}
		
		//TODO
		
		return itemView;
	}

	public void setData(Collection<String> pData)
	{
		synchronized (mData)
		{
			mData.clear();
			mData.addAll(pData);
			this.notifyDataSetChanged();
		}
	}
}
