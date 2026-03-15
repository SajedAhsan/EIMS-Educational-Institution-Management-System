#include<bits/stdc++.h>
using namespace std;
vector<vector<int>> adj_list(1005);
vector<bool> vis(1005, false);
vector<vector<int>> groups;

void bfs(int src){
    vector<int> grp;
    queue<int> q;
    q.push(src);
    vis[src] = true;
    while(!q.empty()){
        int par = q.front();
        vis[par] = true;
        q.pop();
        grp.push_back(par);
        for(int i=0; i<adj_list[par].size(); i++){
            if(!vis[adj_list[par][i]]){
                vis[adj_list[par][i]] = true;
                q.push(adj_list[par][i]);
            }
        }
    }
    groups.push_back(grp);
}
int main(){
    int n;cin >> n;
    int e;cin >> e;
    set<pair<int,int>> done;
    while(e--){
        int a, b;
        cin >> a >> b;
        adj_list[a].push_back(b);
        adj_list[b].push_back(a);
        if(a > b)
            done.insert({b, a});
        else done.insert({a, b});
    }
    for(int i=0; i<n; i++){
        if(!vis[i] && !adj_list[i].empty())
            bfs(i);
    }
    cout << groups.size() << "\n";
    int idx = 1;
    for(int i=0; i<groups.size(); i++){
        //sort(groups[i].begin(), groups[i].end());
        cout << "Group " << idx++ << ": {";
        for(int j=0; j<groups[i].size(); j++){
            cout << groups[i][j];
            if(j == groups[i].size() - 1)
                cout << "} | ";
            else cout << ", ";
        }
        vector<pair<int,int>> not_done;
        for(int j = 0; j<groups[i].size(); j++){
            for(int k = j + 1; k<groups[i].size(); k++){
                int a = groups[i][j], b = groups[i][k];
                if(a > b) swap(a, b);
                if(!done.count({a,b}))
                    not_done.push_back({a, b});
            }
        }
        if(not_done.empty())
            cout << "none\n";
        else{
            for(int j = 0; j<not_done.size(); j++){
                cout << "[" << not_done[j].first << "," << not_done[j].second << "]";
                if(j == not_done.size() - 1)
                    cout << "\n";
                else cout << ", ";
            }
        }
    }
}