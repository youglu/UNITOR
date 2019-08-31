package org.chuniter.core.kernel.kernelunit;

import java.util.Collections;
import java.util.List;

public final class PageIteratorUtil<T> {

	private final NullPageIterator<T> NULL = new NullPageIterator<T>();
	public NullPageIterator<T> getNullPage(){
		NULL.setItems(null);
		return NULL;
	}
	private static class NullPageIterator<T> extends PageIterator<T> {
		private NullPageIterator() {
			this((List<T>)Collections.emptyList(), 0, 0, 0);
		}
		private NullPageIterator(List<T> items, int totalCount,
								 int pageSize, int startIndex) {
			super(items, totalCount, pageSize, startIndex);
		}

		public int getCurrentPage() {
			return 0;
		}

		public int[] getIndexes() {
			return new int[0];
		}

		public List<T> getItems() {
			return Collections.emptyList();
		}

		public int getLastIndex() {
			return 0;
		}

		public int getNextIndex() {
			return 0;
		}

		public int getPageCount() {
			return 0;
		}

		public int getPageSize() {
			return 0;
		}

		public int getPreviousIndex() {
			return 0;
		}

		public int getStartIndex() {
			return 0;
		}

		public int getTotalCount() {
			return 0;
		}
	}

}