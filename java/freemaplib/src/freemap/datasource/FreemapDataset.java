package freemap.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import freemap.data.POI;
import freemap.data.Way;
import freemap.data.Projection;
import freemap.data.Point;
import freemap.data.Annotation;
import freemap.jdem.DEM;

import java.io.PrintWriter;
import java.io.FileWriter;

import java.io.IOException;

public class FreemapDataset implements TiledData
{
	HashMap<Integer,Way> ways;
	HashMap<Integer,POI> pois;
	HashMap<Integer,Annotation> annotations;
	Projection proj;
	
	public interface WayVisitor 
	{
		public void visit(Way w);
	}
	
	public interface AnnotationVisitor
	{
		public void visit(Annotation a);
	}
	
	public FreemapDataset()
	{
		ways=new HashMap<Integer,Way>();
		pois=new HashMap<Integer,POI>();
		annotations=new HashMap<Integer,Annotation>();
	}
	
	public void setProjection(Projection proj)
	{
		this.proj=proj;
	}
	
	public void add(Way way)
	{
		way.reproject(proj);
		ways.put(way.getId(),way);
	}
	
	public void add(POI poi)
	{
		poi.reproject(proj);
		pois.put(poi.getId(),poi);	
	}
	
	public void add(Annotation ann)
	{
		ann.reproject(proj);
		annotations.put(ann.getId(), ann);
	}
	
	public void merge(TiledData otherData)
	{
		ways.putAll(((FreemapDataset)otherData).ways);
		pois.putAll(((FreemapDataset)otherData).pois);
		annotations.putAll(((FreemapDataset)otherData).annotations);
	}
	
	public String toString()
	{
		return "OSMRenderData: " + pois.toString()+"\n" + ways.toString() + "\n" + annotations.toString();
	}
	
	public void applyDEM(DEM dem)
	{
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		Set<Map.Entry<Integer, POI> > poiSet = pois.entrySet();
		
		
		
		for(Map.Entry<Integer,Way> w: waySet)
		{
			System.out.println("way: " + w.getValue().getValue("osm_id"));
			w.getValue().applyDEM(dem);
		}
		
		for(Map.Entry<Integer,POI> p: poiSet)
		{
			p.getValue().applyDEM(dem);
		}
	}
	
	public boolean isWithin(DEM dem)
	{
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		Set<Map.Entry<Integer, POI> > poiSet = pois.entrySet();
		
		for(Map.Entry<Integer,Way> w: waySet)
		{
			if (w.getValue().isWithin(dem))
				return true;
		}
		
		for(Map.Entry<Integer,POI> p: poiSet)
		{
			if(dem.pointWithin(p.getValue().getPoint(),proj))
				return true;
		}
		return true;
	}
	
	public void save(String filename) throws IOException
	{
		PrintWriter pw = new PrintWriter(new FileWriter(filename));
		pw.println("<rdata>");
		writeProjection(pw);
		savePOIs(pw);
		saveWays(pw);
		saveAnnotations(pw);
		pw.println("</rdata>");
		pw.flush();
		pw.close();
	}
	
	public void writeProjection(PrintWriter pw)
	{
		if(proj!=null)
			pw.println("<projection>" + proj.getID()+"</projection>");
	}
	
	public void saveWays(PrintWriter pw)
	{
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		for(Map.Entry<Integer,Way> w: waySet)
		{
			w.getValue().save(pw);
		}
	}
	
	public void savePOIs(PrintWriter pw)
	{
		Set<Map.Entry<Integer, POI> > poiSet = pois.entrySet();
		for(Map.Entry<Integer,POI> p: poiSet)
		{
			p.getValue().save(pw);
		}
	}
	
	public void saveAnnotations(PrintWriter pw)
	{
		Set<Map.Entry<Integer, Annotation> > annSet = annotations.entrySet();
		for(Map.Entry<Integer,Annotation> a: annSet)
		{
			a.getValue().save(pw);
		}
	}
	
	public void reproject(Projection newProj)
	{
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		Set<Map.Entry<Integer, POI> > poiSet = pois.entrySet();
		Set<Map.Entry<Integer, Annotation> > annotationSet = annotations.entrySet();
		
		for(Map.Entry<Integer,Way> w: waySet)
		{
			ways.get(w.getKey()).reproject(newProj);
		}
		for(Map.Entry<Integer,POI> p: poiSet)
		{
			pois.get(p.getKey()).reproject(newProj);
		}
		for(Map.Entry<Integer,Annotation> a: annotationSet)
		{
			annotations.get(a.getKey()).reproject(newProj);
		}
		proj=newProj;
	}
	
	public void operateOnWays(WayVisitor visitor)
	{
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		
		for(Map.Entry<Integer,Way> w: waySet)
			visitor.visit(w.getValue());
	}
	
	public void operateOnAnnotations(AnnotationVisitor visitor)
	{
		Set<Map.Entry<Integer,Annotation> > annSet = annotations.entrySet();
		
		for(Map.Entry<Integer,Annotation> a: annSet)
			visitor.visit(a.getValue());
	}
	
	public void operateOnNearbyWays(WayVisitor visitor, Point point, double distance)
	{
		Way way;
		Set<Map.Entry<Integer,Way> > waySet = ways.entrySet();
		
		for(Map.Entry<Integer,Way> w: waySet)
		{
			way=w.getValue();
			if(way.distanceTo(point)<=distance)
				visitor.visit(way);
		}
	}
	
	public ArrayList<POI> getPOIsByKey(String key)
	{
		return getPOIsByType(key,"*");
	}
	
	public ArrayList<POI> getPOIsByType(String key,String val)
	{
		ArrayList<POI> poiReturned=new ArrayList<POI>();
		Set<Map.Entry<Integer,POI> > poiSet = pois.entrySet();
		for(Map.Entry<Integer,POI> p: poiSet)
		{
			if(p.getValue().containsKey(key) && (val.equals("*") || p.getValue().getValue(key).equals(val)))
				poiReturned.add(p.getValue());
		}
		return poiReturned;
	}
	
	public ArrayList<Annotation> getAnnotations()
	{
		ArrayList<Annotation> anns=new ArrayList<Annotation>();
		Set<Map.Entry<Integer, Annotation> > annSet = annotations.entrySet();
		for(Map.Entry<Integer,Annotation> a: annSet)
		{
			anns.add(a.getValue());
		}
		return anns;
	}
	
	public POI getPOIById(int id)
	{
		return pois.get(id);
	}
	
	public Way getWayById(int id)
	{
		return ways.get(id);
	}
	
	public Annotation getAnnotationById(int id)
	{
		return annotations.get(id);
	}
}


