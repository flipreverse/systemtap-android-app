/*
 * Copyright 2012 Alexander Lochmann, Michael Lenz, Jochen Streicher
 *
 * This file is part of SystemTap4Android.
 *
 * SystemTap4Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SystemTap4Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SystemTap4Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.systemtap.android.service;

import com.systemtap.android.net.SystemTapMessage.ModuleStatus;

public class Module
{	
	private String mName;
	private ModuleStatus mStatus;
	
	public Module(String pName)
	{
		mName = pName;
		mStatus = ModuleStatus.STOPPED;
	}
	
	public String getName() { return mName; }
	
	public void setStatus(ModuleStatus pStatus) { mStatus = pStatus; }
	
	public ModuleStatus getStatus() { return mStatus; }
}
