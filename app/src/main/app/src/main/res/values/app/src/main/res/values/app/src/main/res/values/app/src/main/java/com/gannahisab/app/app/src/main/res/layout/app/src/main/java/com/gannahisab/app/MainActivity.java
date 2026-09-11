package com.gannahisab.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    DB db; TextView summary;
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    String money(double x){return String.format(Locale.US,"%,.0f",x);}
    String num(double x){return String.format(Locale.US,"%.2f",x);}
    double val(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return 0;}}
    EditText field(String hint, boolean numeric){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); return e; }
    LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(20,0,20,0);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    void ask(String title,LinearLayout l,final Runnable save){new AlertDialog.Builder(this).setTitle(title).setView(l).setNegativeButton("منسوخ",null).setPositiveButton("محفوظ کریں",(d,w)->save.run()).show();}

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);db=new DB(this);summary=findViewById(R.id.summary);refresh();
        findViewById(R.id.buy).setOnClickListener(v->purchase());
        findViewById(R.id.trip).setOnClickListener(v->trip());
        findViewById(R.id.expense).setOnClickListener(v->expense());
        findViewById(R.id.payment).setOnClickListener(v->payment());
        findViewById(R.id.receipt).setOnClickListener(v->receipt());
        findViewById(R.id.ledger).setOnClickListener(v->farmerLedger());
        findViewById(R.id.report).setOnClickListener(v->report());
        findViewById(R.id.exportCsv).setOnClickListener(v->exportCsv());
        findViewById(R.id.pdf).setOnClickListener(v->exportPdf());
    }
    void refresh(){
        double buy=db.sum("purchases","total"), paid=db.sum("payments","amount"), expenses=db.sum("expenses","amount");
        double tripCost=db.sum("trips","driverCost")+db.sum("trips","otherCost");
        double sale=db.sum("trips","saleTotal"), received=db.sum("receipts","amount");
        summary.setText("آج: "+today()+"\n\n🌾 کل خرید: "+num(db.sum("purchases","maund"))+" من  |  Rs "+money(buy)+"\n🚛 کل ٹرالے: "+count("trips")+"  |  بھیجا: "+num(db.sum("trips","maund"))+" من\n💵 کل پیمنٹ: Rs "+money(paid)+"\n⛽ ٹرالا خرچہ: Rs "+money(tripCost)+"\n🧾 دیگر خرچہ: Rs "+money(expenses)+"\n🏭 مل کو بل: Rs "+money(sale)+"  |  وصولی: Rs "+money(received)+"\n📌 اندازاً منافع: Rs "+money(sale-buy-tripCost-expenses));
    }
    int count(String t){Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+t,null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}

    void purchase(){LinearLayout l=box();EditText seller=field("کسان/پارٹی کا نام",false), m=field("مقدار (من)",true), r=field("ریٹ فی من",true), p=field("آج ادا شدہ رقم",true);l.addView(seller);l.addView(m);l.addView(r);l.addView(p);ask("🌾 گنا خریداری",l,()->{double mau=val(m),rate=val(r),pay=val(p);db.getWritableDatabase().execSQL("INSERT INTO purchases(date,seller,maund,rate,total,paid) VALUES(?,?,?,?,?,?)",new Object[]{today(),seller.getText().toString(),mau,rate,mau*rate,pay});refresh();});}

    void trip(){LinearLayout l=box();EditText truck=field("ٹرالا نمبر",false), driver=field("ڈرائیور نام",false), m=field("گنا مقدار (من)",true), mill=field("مل کا نام",false), dc=field("ڈرائیور/ٹرالا خرچہ",true), oc=field("دیگر خرچہ",true), sr=field("مل ریٹ فی من",true);l.addView(truck);l.addView(driver);l.addView(m);l.addView(mill);l.addView(dc);l.addView(oc);l.addView(sr);ask("🚛 نیا ٹرالا",l,()->{double mau=val(m),rate=val(sr);db.getWritableDatabase().execSQL("INSERT INTO trips(date,truck,driver,maund,mill,driverCost,otherCost,saleRate,saleTotal,status) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{today(),truck.getText().toString(),driver.getText().toString(),mau,mill.getText().toString(),val(dc),val(oc),rate,mau*rate,"روانہ"});refresh();});}
    void expense(){LinearLayout l=box();EditText t=field("خرچے کی قسم",false),a=field("رقم",true);l.addView(t);l.addView(a);ask("⛽ عام خرچہ",l,()->{db.getWritableDatabase().execSQL("INSERT INTO expenses(date,title,amount) VALUES(?,?,?)",new Object[]{today(),t.getText().toString(),val(a)});refresh();});}
    void payment(){LinearLayout l=box();EditText t=field("کس کسان/پارٹی کو پیمنٹ دی",false),a=field("رقم",true),n=field("نوٹ",false);l.addView(t);l.addView(a);l.addView(n);ask("💰 پیمنٹ",l,()->{db.getWritableDatabase().execSQL("INSERT INTO payments(date,person,amount,note) VALUES(?,?,?,?)",new Object[]{today(),t.getText().toString(),val(a),n.getText().toString()});refresh();});}
    void receipt(){LinearLayout l=box();EditText t=field("مل کا نام",false),a=field("وصول شدہ رقم",true),n=field("نوٹ",false);l.addView(t);l.addView(a);l.addView(n);ask("🏭 مل سے وصولی",l,()->{db.getWritableDatabase().execSQL("INSERT INTO receipts(date,mill,amount,note) VALUES(?,?,?,?)",new Object[]{today(),t.getText().toString(),val(a),n.getText().toString()});refresh();});}

    void farmerLedger(){ final EditText q=field("کسان/پارٹی کا نام",false); new AlertDialog.Builder(this).setTitle("👨‍🌾 کسان کا کھاتہ").setView(q).setNegativeButton("منسوخ",null).setPositiveButton("دیکھیں",(d,w)->showFarmer(q.getText().toString().trim())).show(); }
    void showFarmer(String name){ if(name.isEmpty())return; Cursor c=db.getReadableDatabase().rawQuery("SELECT seller,maund,total,paid FROM purchases WHERE seller LIKE ? ORDER BY id DESC",new String[]{"%"+name+"%"});double total=0,paid=0,mau=0;StringBuilder s=new StringBuilder();while(c.moveToNext()){mau+=c.getDouble(1);total+=c.getDouble(2);paid+=c.getDouble(3);s.append("\n🌾 ").append(num(c.getDouble(1))).append(" من × ").append(money(c.getDouble(2)/Math.max(c.getDouble(1),1))).append(" = Rs ").append(money(c.getDouble(2))).append(" | پیمنٹ ").append(money(c.getDouble(3)));}c.close();Cursor p=db.getReadableDatabase().rawQuery("SELECT amount,note FROM payments WHERE person LIKE ?",new String[]{"%"+name+"%"});while(p.moveToNext()){paid+=p.getDouble(0);s.append("\n💰 اضافی پیمنٹ: Rs ").append(money(p.getDouble(0))).append(" ").append(p.getString(1)==null?"":p.getString(1));}p.close();double bal=total-paid;new AlertDialog.Builder(this).setTitle("کھاتہ: "+name).setMessage("کل گنا: "+num(mau)+" من\nکل خرید: Rs "+money(total)+"\nکل پیمنٹ: Rs "+money(paid)+"\nبقایا: Rs "+money(bal)+"\n"+s).setPositiveButton("ٹھیک ہے",null).show(); }

    void report(){ double buy=db.sum("purchases","total"), tripCost=db.sum("trips","driverCost")+db.sum("trips","otherCost"), exp=db.sum("expenses","amount"), sale=db.sum("trips","saleTotal"), rec=db.sum("receipts","amount"); String msg="🌾 خریداری: Rs "+money(buy)+"\n🚛 ٹرالا: "+count("trips")+"\n📦 کل گنا: "+num(db.sum("trips","maund"))+" من\n🚚 ٹرالا خرچہ: Rs "+money(tripCost)+"\n🧾 دیگر خرچہ: Rs "+money(exp)+"\n🏭 مل کا بل: Rs "+money(sale)+"\n💵 مل سے وصولی: Rs "+money(rec)+"\n\n💰 اندازاً منافع: Rs "+money(sale-buy-tripCost-exp); new AlertDialog.Builder(this).setTitle("📊 مکمل رپورٹ").setMessage(msg).setPositiveButton("ٹھیک ہے",null).show(); }

    String csvEscape(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    void exportCsv(){ try{StringBuilder out=new StringBuilder();out.append("Ganna Hisab Report\n");out.append("Section,Date,Party/Truck/Mill,Quantity Maund,Rate,Total,Payment/Expense,Other\n");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT date,seller,maund,rate,total,paid FROM purchases ORDER BY id",null);while(c.moveToNext())out.append("Purchase,").append(c.getString(0)).append(',').append(csvEscape(c.getString(1))).append(',').append(c.getDouble(2)).append(',').append(c.getDouble(3)).append(',').append(c.getDouble(4)).append(',').append(c.getDouble(5)).append("\n");c.close();
        c=db.getReadableDatabase().rawQuery("SELECT date,truck,maund,saleRate,saleTotal,driverCost,otherCost,mill FROM trips ORDER BY id",null);while(c.moveToNext())out.append("Trip,").append(c.getString(0)).append(',').append(csvEscape(c.getString(1))).append(',').append(c.getDouble(2)).append(',').append(c.getDouble(3)).append(',').append(c.getDouble(4)).append(',').append(c.getDouble(5)).append(',').append(csvEscape(c.getString(7))).append("\n");c.close();
        c=db.getReadableDatabase().rawQuery("SELECT date,title,amount FROM expenses ORDER BY id",null);while(c.moveToNext())out.append("Expense,").append(c.getString(0)).append(',').append(csvEscape(c.getString(1))).append(",,,").append(c.getDouble(2)).append("\n");c.close();
        c=db.getReadableDatabase().rawQuery("SELECT date,person,amount,note FROM payments ORDER BY id",null);while(c.moveToNext())out.append("Payment,").append(c.getString(0)).append(',').append(csvEscape(c.getString(1))).append(",,,").append(c.getDouble(2)).append(',').append(csvEscape(c.getString(3)==null?"":c.getString(3))).append("\n");c.close();
        c=db.getReadableDatabase().rawQuery("SELECT date,mill,amount,note FROM receipts ORDER BY id",null);while(c.moveToNext())out.append("Receipt,").append(c.getString(0)).append(',').append(csvEscape(c.getString(1))).append(",,,").append(c.getDouble(2)).append(',').append(csvEscape(c.getString(3)==null?"":c.getString(3))).append("\n");c.close();
        String name="GannaHisab_"+today()+".csv";ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"text/csv");cv.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);OutputStream os=getContentResolver().openOutputStream(uri);os.write(out.toString().getBytes(StandardCharsets.UTF_8));os.close();cv.clear();cv.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,cv,null,null);Toast.makeText(this,"Excel/CSV رپورٹ Downloads میں محفوظ ہوگئی",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"رپورٹ محفوظ نہیں ہوئی: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    void exportPdf(){try{PdfDocument pdf=new PdfDocument();PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(595,842,1).create();PdfDocument.Page page=pdf.startPage(pi);Canvas can=page.getCanvas();Paint p=new Paint();p.setTextSize(18);p.setTypeface(Typeface.DEFAULT_BOLD);can.drawText("Ganna Hisab - Report",40,55,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(13);double buy=db.sum("purchases","total"),cost=db.sum("trips","driverCost")+db.sum("trips","otherCost"),exp=db.sum("expenses","amount"),sale=db.sum("trips","saleTotal"),rec=db.sum("receipts","amount");String[] lines={"Date: "+today(),"Total Purchase: Rs "+money(buy),"Total Trips: "+count("trips"),"Total Cane Sent: "+num(db.sum("trips","maund"))+" maund","Truck/Driver Cost: Rs "+money(cost),"Other Expenses: Rs "+money(exp),"Mill Bill: Rs "+money(sale),"Mill Receipts: Rs "+money(rec),"Estimated Profit: Rs "+money(sale-buy-cost-exp)};int y=90;for(String line:lines){can.drawText(line,40,y,p);y+=28;}pdf.finishPage(page);String name="GannaHisab_"+today()+".pdf";ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");cv.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);OutputStream os=getContentResolver().openOutputStream(uri);pdf.writeTo(os);os.close();pdf.close();cv.clear();cv.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,cv,null,null);Toast.makeText(this,"PDF رپورٹ Downloads میں محفوظ ہوگئی",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"PDF نہیں بنی: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
}
