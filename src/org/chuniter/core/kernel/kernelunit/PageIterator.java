package org.chuniter.core.kernel.kernelunit;

import java.util.Collections;
import java.util.List;

import org.chuniter.core.kernel.impl.web.BaseServlet; 

public class PageIterator<T>
{
  public static final int PAGESIZE = 30;
  private int pageSize = 30;
  private List<T> items;
  private int totalCount = 0;

  private int[] indexes = new int[0];

  private int startIndex = 1;
  private int nextIndex = 1;
  private int previousIndex = 1;
  private int currentPage = 0;
  private String pagerProMessage;
  
  public String getPagerProMessage() {
	return pagerProMessage;
}

public void setPagerProMessage(String pagerProMessage) {
	this.pagerProMessage = pagerProMessage;
}

public void setCurrentPage(int currentPage) {
	this.currentPage = currentPage;
}

public void setPreviousIndex(int previousIndex) {
	this.previousIndex = previousIndex;
}

public void setNextIndex(int nextIndex) {
	this.nextIndex = nextIndex;
}

private int lastIndex = 1;
  
	public void setLastIndex(int lastIndex) {
		this.lastIndex = lastIndex;
	}

public PageIterator(List<T> items, int totalCount)
  {
    setPageSize(12);
    setTotalCount(totalCount);
    setItems(items);
    setStartIndex(1);
  }

  public PageIterator(List<T> items, int totalCount, int startIndex)
  {
    setPageSize(12);
    setTotalCount(totalCount);
    setItems(items);
    setStartIndex(startIndex);
  }

  public PageIterator(List<T> items, int totalCount, int pageSize, int startIndex)
  {
    setPageSize(pageSize);
    setTotalCount(totalCount);
    setItems(items);
    setStartIndex(startIndex);
    this.getNextIndex();
    this.getLastIndex();
  }

  public List<T> getItems() {
    if (this.items == null) {
      return Collections.emptyList();
    }
    return this.items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public int getTotalCount() {
    return this.totalCount;
  }

  public void setTotalCount(int totalCount)
  {
    if (totalCount > 0) {
      this.totalCount = totalCount;
      int count = totalCount / (this.pageSize<=0?1:this.pageSize);
      if (totalCount % (this.pageSize<=0?1:this.pageSize) > 0)
        ++count;
      this.indexes = new int[count]; 
      for (int i = 0; i < count; ++i)
        this.indexes[i] = (this.pageSize * i);
    }
    else {
      this.totalCount = 0;
    }
  }

  public int[] getIndexes() {
    return this.indexes;
  }

  public void setIndexes(int[] indexes) {
    this.indexes = indexes;
  }

  public int getStartIndex() {
    return this.startIndex;
  }

  public int getPageCount() { 
    if (this.indexes == null||this.indexes.length<=0) {
    	if(this.getTotalCount()>0&&this.pageSize>0){
	    	if(this.getTotalCount()%this.pageSize==0)
	    		return (this.getTotalCount()/this.pageSize);
	    	else
	    		return (this.getTotalCount()/this.pageSize)+1;
    	} 
      return 0;
    }
    return this.indexes.length;
  }

  public void setStartIndex(int startIndex)
  {
	  	this.startIndex = startIndex;
	    if (this.totalCount <= 0){
	      this.startIndex = 1;
	    } else if (startIndex*this.pageSize >= this.totalCount){
	      //this.startIndex = this.indexes[(this.indexes.length - 1)];
	    	
	    } else{
	      this.startIndex = startIndex;//this.indexes[(startIndex / (this.pageSize<=0?1:this.pageSize))];
	    }
  }

  public int getNextIndex()
  { 
    nextIndex =  this.getCurrentPage();// + 1;//this.pageSize;
    if (nextIndex * this.pageSize >= this.totalCount) {
        pagerProMessage = "已经是最后一页了";    	
        return getStartIndex();
    }
    nextIndex+=1;
    return nextIndex;
  }

  public int getPreviousIndex()
  {
    previousIndex = getCurrentPage()-1;//getStartIndex() - this.pageSize;
    if (previousIndex <= 1) {
        pagerProMessage = "已经是第一页了";
      return 1;
    }
    return previousIndex;
  }

  public int getLastIndex() {
    int last = 1;
    int len = this.indexes.length;
    if (len > 0) {
      last = len;// - 1;
      lastIndex = last;//this.indexes[last];
    } else {
      lastIndex = 1;
    }
    return lastIndex;
  }

  public int getCurrentPage() {
    currentPage = 1;
    if (this.totalCount != 0) {
      currentPage = this.startIndex;//(new Double(Math.ceil(this.startIndex / this.pageSize))).intValue();
    }
    if(currentPage <=0)
    	currentPage = 1;
    return currentPage;
  }
  public String toJson(){
	com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
	if(null == this.getItems()||this.getItems().isEmpty()){
		json.put(BaseServlet.STATECODE, StateCode.NODATA);
		return json.toString();
	}
	json.put(BaseServlet.DATA,this.getItems());
	json.put("count",this.getTotalCount());
	json.put("hasnext",(getNextIndex()>this.getTotalCount())?0:1);
	json.put("hasprev",(previousIndex < 0)?0:1);
	return json.toJSONString();
  }
}