/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.mobile.db.providers;

import java.util.Comparator;
import java.util.List;

public class MergeSort {

	private static void mergeSort(Object src[], Object dest[], int low, int high, Comparator c) {
		int length = high - low;

		// Insertion sort on smallest arrays
		if (length < 7) {
			for (int i = low; i < high; i++)
				for (int j = i; j > low && c.compare(dest[j - 1], dest[j]) > 0; j--)
					swap(dest, j, j - 1);
			return;
		}

		// Recursively sort halves of dest into src
		int mid = (low + high) / 2;
		mergeSort(dest, src, low, mid, c);
		mergeSort(dest, src, mid, high, c);

		// If list is already sorted, just copy from src to dest. This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (c.compare(src[mid - 1], src[mid]) <= 0) {
			System.arraycopy(src, low, dest, low, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for (int i = low, p = low, q = mid; i < high; i++) {
			if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
				dest[i] = src[p++];
			else
				dest[i] = src[q++];
		}
	}

	public static <T> void sort(List<T> a1, Comparator<T> c) {
		Object[] a = a1.toArray();
		Object aux[] = a.clone();
		mergeSort(aux, a, 0, aux.length, c);
		synchronized (a1) {

			a1.clear();
			for (Object object : a) {
				a1.add((T) object);
			}
		}
	}

	public static void sort(Object[] a, Comparator c) {
		Object aux[] = a.clone();
		mergeSort(aux, a, 0, a.length, c);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(Object x[], int a, int b) {
		Object t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
}