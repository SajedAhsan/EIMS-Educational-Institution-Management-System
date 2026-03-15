#include<bits/stdc++.h>
using namespace std;
class Coord{
    int x, y;
    public:
    Coord(int a=0, int b=0){x=a, y=b;}
    Coord &operator+=(Coord &ob);
    friend ostream &operator<<(ostream &out, const Coord &ob);
    friend istream &operator>>(istream &in, Coord &ob);

    Coord &operator++(){
        ++x;
        ++y;
        return *this;
    }
    Coord operator++(int unused){
        Coord temp = *this;
        ++x;
        ++y;
        return temp;
    }
};
Coord& Coord :: operator+=(Coord &ob){
    x+=ob.x;
    y+=ob.y;
    return *this;
}
ostream& operator<<(ostream &out, const Coord &ob){
    out << "(" << ob.x << " " << ob.y  << ")";
    return out;
}

istream& operator>>(istream &in, Coord &ob){
    cout<<"Enter coordinated:";
    in >> ob.x >> ob.y;
    return in;
}
int main(){
    Coord a(10, 20), b;
    //int x, y;
    cin >> b;
    cout << b << "\n";
    a += b;
    cout << a << "\n";
    return 0;
}

