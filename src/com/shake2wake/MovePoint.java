/*
 * Copyright 2014 VOVLab
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shake2wake;

public class MovePoint {
	public float x, y, z;

	public void init() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public MovePoint(float x, float y, float z) {

		init();

		this.x = x;
		this.y = y;
		this.z = z;
	}

	public MovePoint(MovePoint point) {

		init();

		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
	}
	
	public MovePoint()
	{
		init();
	}
}
