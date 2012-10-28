package edu.udo.cs.ess.systemtap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import edu.udo.cs.ess.systemtap.service.Module;


public class ModuleSpinnerAdapter extends BaseAdapter implements Observer
{
	private static final String TAG = ModuleSpinnerAdapter.class.getSimpleName();
	
	private ArrayList<Module> mData;
	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	
	public ModuleSpinnerAdapter(Activity pContext)
	{
		mData = new ArrayList<Module>();
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
			return mData.get(pPosition).getName();
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

	public void setData(Collection<Module> pData)
	{
		synchronized (mData)
		{
			mData.clear();
			mData.addAll(pData);
			this.notifyDataSetChanged();
		}
	}
	
	@Override
	public void update(Observable pObservable, final Object pData)
	{
		if (pData instanceof Collection<?>)
		{
			synchronized (mData)
			{
				mActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Collection<Module> newData = (Collection<Module>)pData;
						ModuleSpinnerAdapter.this.mData.clear();
						ModuleSpinnerAdapter.this.mData.addAll(newData);
						ModuleSpinnerAdapter.this.notifyDataSetChanged();
					}
				});
			}
		}
	}
}
