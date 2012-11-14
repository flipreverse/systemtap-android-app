package edu.udo.cs.ess.systemtap;
import java.io.File;
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
import android.widget.TextView;
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
		/* if the user selects the first element and mData is empty, return true, because one dummy item is present */
		if ((pPosition == 0 && mData.size() == 0) || pPosition < mData.size())
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
		/* Although mData is empty at least one dummy item is present  */
		if (mData.size() == 0)
		{
			return 1;
		}
		else
		{
			return mData.size();
		}
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
			 itemView = mLayoutInflater.inflate(R.layout.spinnter_item, pParent, false);
		}
		else
		{
			itemView = pConvertView;
		}

		TextView textViewModuleName = (TextView)itemView.findViewById(R.id.textViewSpinnerModuleName);
		/* No further check of pPosition required. If mData is empty and getView() is called, the ui just requests *one* item which has the text stap_module_list_empty */
		if (mData.size() == 0)
		{
			textViewModuleName.setText(R.string.stap_module_list_empty);
		}
		else
		{
			Module module = mData.get(pPosition);
			textViewModuleName.setText(module.getName());
		}
		
		return itemView;
	}

	public void setData(Collection<Module> pData)
	{
		/* Avoid raceconditions access mData */
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
		/* Some sanity checks to ensure pData is a Collection */
		if (pData instanceof Collection<?>)
		{
			/* Avoid raceconditions accessing mData */
			synchronized (mData)
			{
				/* If the adapter gets new data, a call to notifyDataSetChanged() should be done from the ui thread. */
				/* Only the ui thread is allowed to manipulate the gui */
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
