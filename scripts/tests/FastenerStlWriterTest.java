package scripts.tests;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import ru.ytkab0bp.slicebeam.utils.FastenerStlWriter;
import ru.ytkab0bp.slicebeam.utils.FastenerStandards;

public final class FastenerStlWriterTest {
    public static void main(String[] args) throws Exception {
        File dir=new File("app/build/fastener-tests");if(!dir.exists()&&!dir.mkdirs())throw new AssertionError("test output directory");
        File bolt=new File(dir,"m3-bolt.stl"),nut=new File(dir,"m3-nut.stl"),threadedNut=new File(dir,"m3-threaded-nut.stl"),washer=new File(dir,"m3-washer.stl");
        int bc=FastenerStlWriter.writeBolt(bolt,3,.5f,12,5.5f,2);
        int nc=FastenerStlWriter.writeNut(nut,5.5f,2.4f,3.4f);
        int tnc=FastenerStlWriter.writeThreadedNut(threadedNut,3,.5f,5.5f,2.4f,.2f);
        int wc=FastenerStlWriter.writeWasher(washer,7,3.4f,.8f);
        verify(bolt,bc);verify(nut,nc);verify(threadedNut,tnc);verify(washer,wc);
        File m3FitCoupon=new File(dir,"m3-thread-fit-coupon.stl");
        int couponFacets=FastenerStlWriter.writeThreadFitCoupon(m3FitCoupon,3,.5f,16f,5.5f,2f,2.4f);
        verify(m3FitCoupon,couponFacets,4);
        if(bc<1000||nc<500||tnc<1000||wc<500)throw new AssertionError("unexpectedly sparse geometry");
        expectInvalid(()->FastenerStlWriter.writeWasher(new File(dir,"bad.stl"),4,3.8f,.5f));
        expectInvalid(()->FastenerStlWriter.writeNut(new File(dir,"bad-nut.stl"),5.5f,2,4));
        expectInvalid(()->FastenerStlWriter.writeBolt(new File(dir,"bad-bolt.stl"),3,0,12,5.5f,2));
        expectInvalid(()->FastenerStlWriter.writeBolt(new File(dir,"too-complex-bolt.stl"),2,.25f,40,4,1.5f));
        expectInvalid(()->FastenerStlWriter.writeThreadedNut(new File(dir,"too-complex-nut.stl"),2,.25f,4,24,.2f));
        expectInvalid(()->FastenerStlWriter.writeThreadedNut(new File(dir,"bad-threaded-nut.stl"),3,.5f,5.5f,2.4f,1f));
        check(FastenerStandards.hexNutAcrossFlats(2)==5.5f,"M3 nut standard across-flats preset");
        check(FastenerStandards.coarsePitch(0)==.4f,"M2 coarse pitch preset");
        check(FastenerStandards.washerOuterDiameter(2)==7f,"M3 washer outer-diameter preset");
        check(FastenerStandards.clearanceBore(0)==2.2f,"smallest metric size stays printable");
        check(FastenerStandards.threadClearance(0)<FastenerStandards.threadClearance(1) &&
                FastenerStandards.threadClearance(1)<FastenerStandards.threadClearance(2),
                "thread-fit presets increase from tight to loose");
        check("M3".equals("M" + FastenerStandards.fileNumber(3f)), "nominal size filename label");
        check("12p5".equals(FastenerStandards.fileNumber(12.5f)), "filename decimal separator is locale-independent");
        try { FastenerStandards.fileNumber(Float.NaN); throw new AssertionError("non-finite filename dimension accepted"); }
        catch (IllegalArgumentException expected) { }
        for(int i=0;i<FastenerStandards.count();i++) {
            float bore=FastenerStandards.clearanceBore(i), flats=FastenerStandards.hexNutAcrossFlats(i);
            if(bore>flats-1.3f)throw new AssertionError("default nut clearance leaves too little wall for size index "+i);
            File sizeBolt=new File(dir,"standard-bolt-"+i+".stl");
            int boltFacets=FastenerStlWriter.writeBolt(sizeBolt,FastenerStandards.diameter(i),
                    FastenerStandards.coarsePitch(i),16f,flats,
                    Math.max(1.2f,FastenerStandards.diameter(i)*.67f));
            verify(sizeBolt,boltFacets);
            File sizeNut=new File(dir,"standard-nut-"+i+".stl");
            int facets=FastenerStlWriter.writeThreadedNut(sizeNut,FastenerStandards.diameter(i),
                    FastenerStandards.coarsePitch(i),flats,FastenerStandards.nutHeight(i),.2f);
            verify(sizeNut,facets);
            for(int fit=0;fit<3;fit++) {
                File fitNut=new File(dir,"standard-nut-"+i+"-fit-"+fit+".stl");
                int fitFacets=FastenerStlWriter.writeThreadedNut(fitNut,
                        FastenerStandards.diameter(i),FastenerStandards.coarsePitch(i),flats,
                        FastenerStandards.nutHeight(i),FastenerStandards.threadClearance(fit));
                verify(fitNut,fitFacets);
            }
            File sizeWasher=new File(dir,"standard-washer-"+i+".stl");
            int washerFacets=FastenerStlWriter.writeWasher(sizeWasher,
                    FastenerStandards.washerOuterDiameter(i),bore,FastenerStandards.washerThickness(i));
            verify(sizeWasher,washerFacets);
            File fitCoupon=new File(dir,"standard-fit-coupon-"+i+".stl");
            int coupon=FastenerStlWriter.writeThreadFitCoupon(fitCoupon,FastenerStandards.diameter(i),
                    FastenerStandards.coarsePitch(i),16f,flats,
                    Math.max(1.2f,FastenerStandards.diameter(i)*.67f),FastenerStandards.nutHeight(i));
            verify(fitCoupon,coupon,4);
        }
        System.out.println("FastenerStlWriterTest passed: M2-M12 hardware, 27 thread fits and four-shell comparison coupons.");
    }
    private static void verify(File file,int expected)throws Exception{verify(file,expected,1);}
    private static void verify(File file,int expected,int expectedComponents)throws Exception{
        double signedVolume=0;java.util.Map<String,Integer> edges=new java.util.HashMap<>();
        java.util.Map<String,String> parents=new java.util.HashMap<>();
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){
            if(in.length()!=84L+50L*expected)throw new AssertionError("STL size/facet count mismatch: "+file);
            in.seek(80);byte[] b=new byte[4];in.readFully(b);int count=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();if(count!=expected)throw new AssertionError("facet count mismatch");
            for(int i=0;i<count;i++){byte[] rec=new byte[50];in.readFully(rec);ByteBuffer q=ByteBuffer.wrap(rec).order(ByteOrder.LITTLE_ENDIAN);float nx=q.getFloat(),ny=q.getFloat(),nz=q.getFloat();if(!Float.isFinite(nx)||!Float.isFinite(ny)||!Float.isFinite(nz))throw new AssertionError("invalid normal");double norm=Math.sqrt(nx*nx+ny*ny+nz*nz);if(norm<.99||norm>1.01)throw new AssertionError("non-unit normal");float[] v=new float[9];for(int k=0;k<9;k++){v[k]=q.getFloat();if(!Float.isFinite(v[k]))throw new AssertionError("invalid vertex");}double area=area(v);if(area<1e-7)throw new AssertionError("degenerate triangle");signedVolume+=(v[0]*(v[4]*v[8]-v[5]*v[7])+v[1]*(v[5]*v[6]-v[3]*v[8])+v[2]*(v[3]*v[7]-v[4]*v[6]))/6.0;String a=vertex(v,0),vb=vertex(v,3),c=vertex(v,6);edge(edges,a,vb);edge(edges,vb,c);edge(edges,c,a);union(parents,a,vb);union(parents,a,c);}
        }
        if(signedVolume<=0)throw new AssertionError("inward-facing or invalid closed surface: "+file+" volume="+signedVolume);
        for(int uses:edges.values())if(uses!=2)throw new AssertionError("non-manifold/open edge: "+file+" uses="+uses);
        java.util.Set<String> components=new java.util.HashSet<>();for(String vertex:parents.keySet())components.add(root(parents,vertex));
        if(components.size()!=expectedComponents)throw new AssertionError("expected "+expectedComponents+
                " connected shells but found "+components.size()+": "+file);
    }
    private static double area(float[]v){double ax=v[3]-v[0],ay=v[4]-v[1],az=v[5]-v[2],bx=v[6]-v[0],by=v[7]-v[1],bz=v[8]-v[2];return Math.sqrt(Math.pow(ay*bz-az*by,2)+Math.pow(az*bx-ax*bz,2)+Math.pow(ax*by-ay*bx,2));}
    private static String vertex(float[] v,int i){return Integer.toHexString(Float.floatToIntBits(v[i]))+":"+Integer.toHexString(Float.floatToIntBits(v[i+1]))+":"+Integer.toHexString(Float.floatToIntBits(v[i+2]));}
    private static void edge(java.util.Map<String,Integer> map,String a,String b){String key=a.compareTo(b)<0?a+"|"+b:b+"|"+a;map.put(key,map.getOrDefault(key,0)+1);}
    private static String root(java.util.Map<String,String> parents,String value){String parent=parents.get(value);if(parent==null){parents.put(value,value);return value;}if(parent.equals(value))return value;String root=root(parents,parent);parents.put(value,root);return root;}
    private static void union(java.util.Map<String,String> parents,String a,String b){String ra=root(parents,a),rb=root(parents,b);if(!ra.equals(rb))parents.put(ra,rb);}
    private static void expectInvalid(Throwing action)throws Exception{try{action.run();throw new AssertionError("invalid input accepted");}catch(java.io.IOException expected){}}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    private interface Throwing{void run()throws Exception;}
}
