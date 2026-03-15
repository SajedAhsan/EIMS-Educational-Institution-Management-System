#include<bits/stdc++.h>
using namespace std;
int main()
{
    int node,edge;
    cin >> node >>edge;
    vector<int> array_of_vector[node];
    while(edge--)
    {
        int a,b;cin>>a>>b;
        array_of_vector[a].push_back(b);
        array_of_vector[b].push_back(a);//undirected graph er jonno only;
    }
    for(int i=0; i<node; i++)
    {
        cout<< i << "->" ;
        for(int a : array_of_vector[i])
        {
            cout<<a<<" ";
        }
        cout<<endl;
    }
}