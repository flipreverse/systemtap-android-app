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
import android.widget.TextView;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.Module;


public class ModuleListAdapter extends BaseAdapter implements Observer
{
	private static final String TAG = ModuleListAdapter.class.getSimpleName();
	
	private ArrayList<Module> mData;
	private Activity mActivity;
	private LayoutInflater mLayoutInflater;
	
	public ModuleListAdapter(Activity pContext)
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
		
		Module module = mData.get(pPosition);
		if (module == null)
		{
			Eventlog.e(TAG,"caller requests unknown item: " + pPosition);
			return null;
		}
		
		TextView textViewModulename = (TextView)itemView.findViewById(R.id.textViewModuleName);
		textViewModulename.setText(module.getName());
		
		TextView textViewModuleStatus = (TextView)itemView.findViewById(R.id.textViewModuleStatus);
		/* Depending on a modules status set the displayed text and its color */
		switch(module.getStatus())
		{
			case RUNNING:
				textViewModuleStatus.setText(mActivity.getText(R.string.stap_module_running));
				textViewModuleStatus.setTextColor(mActivity.getResources().getColor(R.color.stap_module_running));
				break;

			case STOPPED:
				textViewModuleStatus.setText(mActivity.getText(R.string.stap_module_stopped));
				textViewModuleStatus.setTextColor(mActivity.getResources().getColor(R.color.stap_module_stopped));
				break;

			case CRASHED:
				textViewModuleStatus.setText(mActivity.getText(R.string.stap_module_crashed));
				textViewModuleStatus.setTextColor(mActivity.getResources().getColor(R.color.stap_module_crashed));
				break;
		}
		
		return itemView;
	}

	public void setData(Collection<Module> pData)
	{
		/* Avoid raceconditions accessing mData */
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
						ModuleListAdapter.this.mData.clear();
						ModuleListAdapter.this.mData.addAll(newData);
						ModuleListAdapter.this.notifyDataSetChanged();
					}
				});
			}
		}
	}	
}
