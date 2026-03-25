import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DictionaryItem, RuleWeights, TemplateConfig } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AdminControlService {
  constructor(private readonly http: HttpClient) {}

  getWeights(): Observable<RuleWeights> {
    return this.http.get<RuleWeights>('/api/admin/control/search-weights');
  }

  updateWeights(payload: RuleWeights): Observable<RuleWeights> {
    return this.http.put<RuleWeights>('/api/admin/control/search-weights', payload);
  }

  listTemplates(): Observable<TemplateConfig[]> {
    return this.http.get<TemplateConfig[]>('/api/admin/control/templates');
  }

  createTemplate(payload: Omit<TemplateConfig, 'id'>): Observable<TemplateConfig> {
    return this.http.post<TemplateConfig>('/api/admin/control/templates', payload);
  }

  updateTemplate(id: number, payload: Omit<TemplateConfig, 'id'>): Observable<TemplateConfig> {
    return this.http.put<TemplateConfig>(`/api/admin/control/templates/${id}`, payload);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/control/templates/${id}`);
  }

  listDictionary(category?: string): Observable<DictionaryItem[]> {
    if (category) {
      return this.http.get<DictionaryItem[]>('/api/admin/control/dictionaries', { params: { category } });
    }
    return this.http.get<DictionaryItem[]>('/api/admin/control/dictionaries');
  }

  createDictionary(payload: Omit<DictionaryItem, 'id'>): Observable<DictionaryItem> {
    return this.http.post<DictionaryItem>('/api/admin/control/dictionaries', payload);
  }

  updateDictionary(id: number, payload: Omit<DictionaryItem, 'id'>): Observable<DictionaryItem> {
    return this.http.put<DictionaryItem>(`/api/admin/control/dictionaries/${id}`, payload);
  }

  deleteDictionary(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/control/dictionaries/${id}`);
  }
}
