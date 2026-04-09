#include<bits/stdc++.h>
using namespace std;
class Myclass{
    int a;
    public:
    Myclass(int n){
        a = n;
    }
    Myclass(char * str){
        a = atoi(str);
    }
    int getA(){return a;}
};
int main(){
    Myclass ob1(10);
// Myclass ob2 = 20; Error, Why? -> karon sir er slide e explicit ase
Myclass ob3("40");
Myclass ob4 = "60"; // Ok, Why?
Myclass ob2 = 20;
//Auto - Boxing
cout << "ob1: " << ob1.getA() << endl;
cout << "ob3: " << ob3.getA() << endl;
cout << "ob4: " << ob4.getA() << endl;
cout << "ob2: " << ob2.getA() << endl;
return 0;
}